package net.joshuahughes.imagej;

import java.awt.Paint;
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

import net.joshuahughes.imagej.parameter.BooleanBoxParameter;
import net.joshuahughes.imagej.parameter.ColormapBoxParameter;
import net.joshuahughes.imagej.parameter.NumberSpinnerParameter;
import net.joshuahughes.imagej.parameter.PaintBoxParameter;
import net.joshuahughes.imagej.parameter.Parameter;
import net.joshuahughes.imagej.parameter.StringFieldParameter;

import org.reflections.Reflections;

import com.jhlabs.image.Colormap;

public class JHLabsPlugin extends AbstractPlugin<BufferedImageOp>{
	private Parameter get(Method method,Object obj) {
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
	private void setFields(BufferedImageOp op) {
		LinkedHashMap<String,Method> methodMap = new LinkedHashMap<String,Method>();
		for(Method method : op.getClass().getMethods())
			methodMap.put(method.getName(),method);
		for(Parameter parameter : parameterMap.get(op)){
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
	@Override
	protected String getName(BufferedImageOp t) {
		return t.getClass().getSimpleName();
	}
	@Override
	protected BufferedImage getImage(BufferedImage image, BufferedImageOp t,LinkedHashSet<Parameter> list) {
		setFields(t);
		return t.filter(image,null);
	}
	@Override
	protected LinkedHashMap<BufferedImageOp, LinkedHashSet<Parameter>> getMap() {
		LinkedHashMap<BufferedImageOp, LinkedHashSet<Parameter>> map = new LinkedHashMap<BufferedImageOp, LinkedHashSet<Parameter>>();
		Vector<BufferedImageOp> biOps = getAll("com.jhlabs",BufferedImageOp.class);
		Collections.sort(biOps,new Comparator<BufferedImageOp>(){
			public int compare(BufferedImageOp o1, BufferedImageOp o2) {
				return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
			}});
		for(BufferedImageOp op : biOps){
			map.put(op,new LinkedHashSet<Parameter>());
			LinkedHashMap<String,Method> methodNameMap = new LinkedHashMap<String,Method>();
			for(Method m : op.getClass().getMethods())
				methodNameMap.put(m.getName(),m);
			for(Entry<String, Method> e : methodNameMap.entrySet())
				if(e.getKey().startsWith("get") && e.getValue().getParameterTypes().length == 0){
					if(methodNameMap.keySet().contains("s"+e.getKey().substring(1, e.getKey().length()))){
						Parameter parameter = get(e.getValue(),op);
						if(parameter != null)
							map.get(op).add(parameter);
					}
				}
		}
		return map;
	}

}
