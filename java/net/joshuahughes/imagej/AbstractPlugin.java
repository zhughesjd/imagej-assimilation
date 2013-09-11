package net.joshuahughes.imagej;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.joshuahughes.imagej.parameter.Parameter;

import org.reflections.Reflections;

public abstract class AbstractPlugin<T> implements ExtendedPlugInFilter,ActionListener{
	int FLAGS = PlugInFilter.DOES_ALL;

	protected LinkedHashMap<T,LinkedHashSet<Parameter>> parameterMap = getMap();
	private JDialog dialog = new JDialog();
	private JList list = new JList();
	private ImagePlus source;
	private ImagePlus result = new ImagePlus("Result");
	protected abstract String getName(T t);
	protected abstract LinkedHashMap<T, LinkedHashSet<Parameter>> getMap();
	protected abstract BufferedImage getImage(BufferedImage image,T t,LinkedHashSet<Parameter> list);
	public static <T> Vector<T> getAll(String packagename,Class<T> clazz){
		Reflections reflections = new Reflections(packagename);
		Vector<T> vector = new Vector<T>();
		ArrayList<Class<? extends T>> set = new ArrayList<Class<? extends T>>();
		set.addAll(reflections.getSubTypesOf(clazz));
		try {
			for(Class<? extends T> c : set){
				Constructor<?> nullConstructor = null;
				for(Constructor<?> candidate : c.getConstructors())
					if(candidate.getParameterTypes().length == 0)
						nullConstructor = candidate;
				if(nullConstructor != null && !Modifier.isAbstract( c.getModifiers() )){
					T biOp = c.getConstructor((Class<?>[])null).newInstance((Object[])null);
					vector.add(biOp);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return vector;
	}
	@Override
	public int setup(String arg, ImagePlus imp) {
		return FLAGS;
	}
	@Override
	public void run(ImageProcessor ip) {

	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.source = imp;
		DefaultListModel model = new DefaultListModel();
		final LinkedHashMap<T,JPanel> pnlMap = new LinkedHashMap<T,JPanel>();
		for(Entry<T, LinkedHashSet<Parameter>> entry : parameterMap.entrySet()){
			model.addElement(entry.getKey());
			JPanel pnl = new JPanel(new GridBagLayout());
			pnlMap.put(entry.getKey(),pnl);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=gbc.gridy=0;
			gbc.weightx=gbc.weighty=1;
			for(Parameter parameter : entry.getValue()){
				parameter.addActionListener(this);
				pnl.add(new JLabel(parameter.getName()),gbc);
				gbc.gridx++;
				pnl.add(parameter.getComponent(),gbc);
				gbc.gridy++;
				gbc.gridx=0;
			}
		}
		list.setModel(model);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(new JScrollPane(list),BorderLayout.WEST);
		dialog.getContentPane().add(new JPanel(),BorderLayout.CENTER);
		dialog.setSize(500,500);
		list.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				actionPerformed(new ActionEvent(e.getSource(),ActionEvent.ACTION_FIRST,e.toString()));
			}
		});
		list.setCellRenderer(new ListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				@SuppressWarnings("unchecked")
				JLabel lbl = new JLabel(getName((T)value));
				lbl.setOpaque(true);
				lbl.setBackground(isSelected?Color.gray:lbl.getBackground());
				return lbl;
			}});
		list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				dialog.getContentPane().removeAll();
				dialog.getContentPane().add(new JScrollPane(list),BorderLayout.WEST);
				dialog.getContentPane().add(pnlMap.get(list.getSelectedValue()),BorderLayout.CENTER);
				dialog.getContentPane().validate();
				dialog.repaint();
				list.requestFocus();
			}
		});
		list.setSelectedIndex(0);
		dialog.setVisible(true);
		return FLAGS;
	}
	@Override
	public void setNPasses(int nPasses) {

	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if(source == null) return;
		@SuppressWarnings("unchecked")
		T t = (T) list.getSelectedValue();
		ImageStack resultStack = new ImageStack(source.getStack().getProcessor(1).getWidth(),source.getStack().getProcessor(1).getHeight());
		for(int imgNdx=1;imgNdx<=source.getStack().getSize();imgNdx++){
			BufferedImage resultImg = getImage(source.getStack().getProcessor(imgNdx).getBufferedImage(),t,this.parameterMap.get(t));
			resultStack.addSlice(new ImagePlus("image:"+imgNdx,resultImg).getChannelProcessor());
		}
		result.setStack("JH Labs Result",resultStack);
		if(result.isVisible())
			result.repaintWindow();
		else
			result.show();

	}
}
