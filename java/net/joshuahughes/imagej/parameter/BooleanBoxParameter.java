package net.joshuahughes.imagej.parameter;

import javax.swing.JCheckBox;


public class BooleanBoxParameter extends JCheckBox implements Parameter{
	private static final long serialVersionUID = 726104080593868769L;
	public BooleanBoxParameter(String name,boolean b){
		super("",b);
		setName(name);
	}
	public Boolean getCurrentValue() {
		return this.isSelected();
	}

	public JCheckBox getComponent() {
		return this;
	}
}
