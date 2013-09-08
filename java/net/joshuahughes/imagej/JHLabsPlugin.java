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
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import net.joshuahughes.imagej.parameter.BooleanBoxParameter;
import net.joshuahughes.imagej.parameter.ColormapBoxParameter;
import net.joshuahughes.imagej.parameter.NumberSpinnerParameter;
import net.joshuahughes.imagej.parameter.PaintBoxParameter;
import net.joshuahughes.imagej.parameter.Parameter;
import net.joshuahughes.imagej.parameter.StringFieldParameter;

import org.reflections.Reflections;

import com.jhlabs.image.Colormap;

public class JHLabsPlugin implements ExtendedPlugInFilter,ActionListener{
	int FLAGS = PlugInFilter.DOES_ALL;

	private LinkedHashMap<BufferedImageOp,JPanel> pnlMap = new LinkedHashMap<BufferedImageOp,JPanel>();
	private LinkedHashMap<BufferedImageOp,LinkedHashSet<Parameter<?,?>>> cmpSet = new LinkedHashMap<BufferedImageOp,LinkedHashSet<Parameter<?,?>>>();
	private JDialog jaiDlg = new JDialog();
	private JList list = new JList();
	private ImagePlus source;
	private ImagePlus result = new ImagePlus("Result");
	private Parameter<?,?> get(Method method,Object obj) {
		String loName = Character.toLowerCase(method.getName().charAt(3))+method.getName().substring(4, method.getName().length());
		method.setAccessible(true);
		try {
			if(method.getReturnType().equals(long.class))
				return new NumberSpinnerParameter<Long>(loName,(Long) method.invoke(obj));
			if(method.getReturnType().equals(int.class))
				return new NumberSpinnerParameter<Integer>(loName,(Integer) method.invoke(obj));
			if(method.getReturnType().equals(double.class))
				return new NumberSpinnerParameter<Double>(loName,(Double) method.invoke(obj));
			if(method.getReturnType().equals(float.class))
				return new NumberSpinnerParameter<Float>(loName,(Float) method.invoke(obj));
			if(method.getReturnType().equals(boolean.class))
				return new BooleanBoxParameter(loName,(Boolean) method.invoke(obj));
			if(method.getReturnType().equals(Colormap.class))
				return new ColormapBoxParameter(loName);
			if(method.getReturnType().equals(Paint.class))
				return new PaintBoxParameter(loName);
			if(method.getReturnType().equals(String.class))
				return new StringFieldParameter(loName,method.invoke(obj) == null?"":method.invoke(obj).toString());
		} catch (Exception e) {
			System.out.println(method.getName()+"\t"+obj.getClass());
			e.printStackTrace();
		}
		return null;
	}
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
		Vector<BufferedImageOp> biOps = getAll("com.jhlabs",BufferedImageOp.class);
		Collections.sort(biOps,new Comparator<BufferedImageOp>(){
			public int compare(BufferedImageOp o1, BufferedImageOp o2) {
				return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
			}});
		DefaultListModel model = new DefaultListModel();
		for(BufferedImageOp op : biOps){
			model.addElement(op);
			JPanel pnl = new JPanel(new GridBagLayout());
			pnlMap.put(op,pnl);
			cmpSet.put(op,new LinkedHashSet<Parameter<?,?>>());
			LinkedHashMap<String,Method> methodNameMap = new LinkedHashMap<String,Method>();
			for(Method m : op.getClass().getMethods())
				methodNameMap.put(m.getName(),m);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=gbc.gridy=0;
			gbc.weightx=gbc.weighty=1;
			for(Entry<String, Method> e : methodNameMap.entrySet())
				if(e.getKey().startsWith("get") && e.getValue().getParameterTypes().length == 0){
					if(methodNameMap.keySet().contains("s"+e.getKey().substring(1, e.getKey().length()))){
						Parameter<?,?> parameter = get(e.getValue(),op);
						if(parameter != null){
							parameter.addActionListener(this);
							cmpSet.get(op).add(parameter);
							pnl.add(new JLabel(parameter.getName()),gbc);
							gbc.gridx++;
							pnl.add(parameter.getComponent(),gbc);
							gbc.gridy++;
							gbc.gridx=0;
						}
					}
				}
		}
		list.setModel(model);
		jaiDlg.getContentPane().setLayout(new BorderLayout());
		jaiDlg.getContentPane().add(new JScrollPane(list),BorderLayout.WEST);
		jaiDlg.getContentPane().add(new JPanel(),BorderLayout.CENTER);
		jaiDlg.setSize(500,500);
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
				JLabel lbl = new JLabel(value.getClass().getSimpleName());
				lbl.setOpaque(true);
				lbl.setBackground(isSelected?Color.gray:lbl.getBackground());
				return lbl;
			}});
		list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				jaiDlg.getContentPane().removeAll();
				jaiDlg.getContentPane().add(new JScrollPane(list),BorderLayout.WEST);
				jaiDlg.getContentPane().add(pnlMap.get(list.getSelectedValue()),BorderLayout.CENTER);
				jaiDlg.getContentPane().validate();
				jaiDlg.repaint();
				list.requestFocus();
			}
		});
		list.setSelectedIndex(0);
		jaiDlg.setVisible(true);
		return FLAGS;
	}
	@Override
	public void setNPasses(int nPasses) {

	}
	@Override
	public void actionPerformed(ActionEvent e) {
		BufferedImageOp op = (BufferedImageOp) list.getSelectedValue();
		setFields(op);
		if(source == null) return;
		ImageStack resultStack = new ImageStack(source.getStack().getProcessor(1).getWidth(),source.getStack().getProcessor(1).getHeight());
		for(int imgNdx=1;imgNdx<=source.getStack().getSize();imgNdx++){
			BufferedImage resultImg = op.filter(source.getStack().getProcessor(imgNdx).getBufferedImage(),null);
			resultStack.addSlice(new ImagePlus("image:"+imgNdx,resultImg).getChannelProcessor());
		}
		result.setStack("JH Labs Result",resultStack);
		if(result.isVisible())
			result.repaintWindow();
		else
			result.show();

	}
	private void setFields(BufferedImageOp op) {

		LinkedHashMap<String,Method> methodMap = new LinkedHashMap<String,Method>();
		for(Method method : op.getClass().getMethods())
			methodMap.put(method.getName(),method);
		for(Parameter<?,?> parameter : cmpSet.get(op)){
			String setMethodName = "set"+Character.toUpperCase(parameter.getName().charAt(0))+parameter.getName().substring(1);
			Method method = methodMap.get(setMethodName);
			try {
				method.invoke(op,parameter.getCurrentValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args){
		new JHLabsPlugin().showDialog(null,null,null);
	}

}
