package net.joshuahughes.imagej.parameter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class NumberSpinnerParameter<N extends Number>  extends JSpinner implements Parameter<N,JSpinner>{
	private static enum Type{min,max,increment}
	private static final long serialVersionUID = 4948588021187942429L;
	SpinnerNumberModel model;
	public NumberSpinnerParameter(String name,N n){
		setName(name);
		setModel(model = new SpinnerNumberModel(n,get(n,Type.min),get(n,Type.max),(Number)get(n,Type.increment)));
	}
	@SuppressWarnings("unchecked")
	private Comparable<N> get(N n,Type t) {
		if(n.getClass().equals(Double.class))
			return (Comparable<N>)(Double)( t.equals(Type.min)?Double.NEGATIVE_INFINITY:(t.equals(Type.max)?Double.POSITIVE_INFINITY:.001d));
		if(n.getClass().equals(Float.class))
			return (Comparable<N>)(Float)( t.equals(Type.min)?Float.NEGATIVE_INFINITY:(t.equals(Type.max)?Float.POSITIVE_INFINITY:.001f));
		if(n.getClass().equals(Long.class))
			return (Comparable<N>)(Long)( t.equals(Type.min)?Long.MIN_VALUE:(t.equals(Type.max)?Long.MAX_VALUE:1l));
		if(n.getClass().equals(Integer.class))
			return (Comparable<N>)(Integer)( t.equals(Type.min)?Integer.MIN_VALUE:(t.equals(Type.max)?Integer.MAX_VALUE:1));
		return null;
	}
	public JSpinner getComponent() {
		return this;
	}

	public void addActionListener(ActionListener al) {
		final ActionListener finalAl = al;
		this.addChangeListener(new ChangeListener(){
			public void stateChanged(final ChangeEvent e) {
				finalAl.actionPerformed(new ActionEvent(NumberSpinnerParameter.this, ActionEvent.ACTION_PERFORMED, ""));
			}
		});
	}
	@SuppressWarnings("unchecked")
	public N getCurrentValue() {
		return (N) model.getValue();
	}
}
