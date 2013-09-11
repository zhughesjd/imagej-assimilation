package net.joshuahughes.imagej;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import net.joshuahughes.hipr2.lower.image;
import net.joshuahughes.hipr2.lower.image1DInt;
import net.joshuahughes.hipr2.upper.BinaryFast;
import net.joshuahughes.hipr2.upper.TwoDArray;
import net.joshuahughes.hipr2.upper.TwoImages;
import net.joshuahughes.imagej.parameter.BooleanBoxParameter;
import net.joshuahughes.imagej.parameter.NumberSpinnerParameter;
import net.joshuahughes.imagej.parameter.Parameter;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class HIPR2Plugin extends AbstractPlugin<Method>{
	public static LinkedHashSet<Class<?>> imageClassArray = new LinkedHashSet<Class<?>>(Arrays.<Class<?>>asList(int[].class,int[][].class,double[].class,double[][].class,BinaryFast.class,TwoImages.class,TwoDArray.class));
	@Override
	protected String getName(Method t) {
		return t.getDeclaringClass().getSimpleName()+"."+t.getName();
	}
	@Override
	protected LinkedHashMap<Method,LinkedHashSet<Parameter>> getMap() {
		LinkedHashMap<Method,LinkedHashSet<Parameter>> map = new LinkedHashMap<Method,LinkedHashSet<Parameter>>();
		List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
		classLoadersList.add(ClasspathHelper.contextClassLoader());
		classLoadersList.add(ClasspathHelper.staticClassLoader());

		Reflections reflections = new Reflections(new ConfigurationBuilder()
		    .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
		    .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
		    .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("net.joshuahughes.hipr2.upper"))));
		LinkedHashSet<Class<? extends Object>> classSet = new LinkedHashSet<Class<? extends Object>>();
		classSet.addAll(reflections.getSubTypesOf(Object.class));
		classSet.addAll(reflections.getSubTypesOf(Thread.class));
		LinkedHashSet<Method> methodSet = new LinkedHashSet<Method>();
		for(Class<? extends Object> clazz : classSet){
			LinkedHashSet<Method> candidateSet = new LinkedHashSet<Method>();
			candidateSet.addAll(Arrays.asList(clazz.getMethods()));
			candidateSet.addAll(Arrays.asList(clazz.getDeclaredMethods()));
			for(Method method : candidateSet)
				if(imageClassArray.contains(method.getReturnType()) && method.getParameterTypes().length>0 && imageClassArray.contains(method.getParameterTypes()[0]))
					methodSet.add(method);
		}
		for(Method method : methodSet)
			map.put(method,getParameterSet(method));
		return map;
	}

	private LinkedHashSet<Parameter> getParameterSet(Method method) {
		LinkedHashSet<Parameter> set = new LinkedHashSet<Parameter>();
		for(int ndx=1;ndx<method.getParameterTypes().length;ndx++)
			if(ndx!=1 || !imageClassArray.contains(method.getParameterTypes()[ndx])){
				Class<?> pClass = method.getParameterTypes()[ndx];
				String name = "param "+ ndx; 
				if(pClass.equals(long.class))
					set.add(new NumberSpinnerParameter<Long>(name,1l));
				if(pClass.equals(int.class))
					set.add(new NumberSpinnerParameter<Integer>(name,1));
				if(pClass.equals(double.class))
					set.add(new NumberSpinnerParameter<Double>(name,1d));
				if(pClass.equals(float.class))
					set.add(new NumberSpinnerParameter<Float>(name,1f));
				if(pClass.equals(boolean.class))
					set.add(new BooleanBoxParameter(name,true));
			}
		System.out.println(set.size());
		return set;
	}
	@Override
	protected BufferedImage getImage(BufferedImage image, Method t,LinkedHashSet<Parameter> set) {
		try {
			Object[] values = new Object[set.size()];
			int ndx=0;
			for(Parameter parameter : set)
				values[ndx++] = parameter.getCurrentValue();
			Object instance = null;
			for(Constructor<?> constructor : t.getDeclaringClass().getConstructors())
				if(constructor.getParameterTypes().length == 0){
					try{instance = constructor.newInstance((Object)null);}catch(Exception exception){exception.printStackTrace();}
					break;
				}
				else if(constructor.getParameterTypes().length == 2){
					try{instance = constructor.newInstance(image.getWidth(),image.getHeight());}catch(Exception exception){exception.printStackTrace();}
					break;
				}
			return toBufferedImage(t.invoke(instance,values));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private BufferedImage toBufferedImage(Object invoke) {
		return null;
	}
	public static void main(String[] args){
		new HIPR2Plugin().showDialog(null,null,null);
	}
	public static final image1DInt BufferedImageToImage(BufferedImage source){
		int width = source.getWidth();
		int height = source.getHeight();
		int [] inputImage = new int [width*height];
		PixelGrabber grabber = new PixelGrabber(source,
				0,0,
				width,
				height,
				inputImage,
				0,width);
		try{
			grabber.grabPixels();
			for(int i=0;i<inputImage.length;++i){
				int pixel = inputImage[i];
				inputImage[i] = (new Color(pixel)).getRGB();
			}
			return new image1DInt(width,
					height,
					inputImage);
		}catch(InterruptedException e){
		}
		return null;
	}
	public static final BufferedImage ImageToBufferedImage(image source){
		int width = source.getWidth();
		int height = source.getHeight();
		int type = BufferedImage.TYPE_3BYTE_BGR;
		int[] values = source.getValues();
		if(source instanceof BinaryFast){
			BinaryFast bf = (BinaryFast) source;
			width = bf.w;
			height = bf.h;
			type = BufferedImage.TYPE_BYTE_BINARY;
			values = bf.convertToArray();
		}
		
		BufferedImage result = new BufferedImage(width,height,type);
		int index=0;
		for(int y=0;y<result.getHeight();y++)
			for(int x=0;x<result.getWidth();x++){
				result.setRGB(x, y, values[index++]);
			}
		return result;
	}

}
