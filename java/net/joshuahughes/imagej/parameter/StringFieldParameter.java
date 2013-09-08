package net.joshuahughes.imagej.parameter;

import java.awt.Dimension;

import javax.swing.JTextField;


public class StringFieldParameter extends JTextField implements Parameter<String,JTextField>{
	private static final long serialVersionUID = 726104080593868769L;
	public StringFieldParameter(String name,String string){
		super(string == null?"":string);
		setPreferredSize(new Dimension(200,(int)getPreferredSize().getHeight()));
		setName(name);
	}
	public String getCurrentValue() {
		return getText();
	}

	public JTextField getComponent() {
		return this;
	}
}
