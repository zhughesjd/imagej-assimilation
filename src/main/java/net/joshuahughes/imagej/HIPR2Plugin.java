package net.joshuahughes.imagej;

import ij.IJ;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import net.joshuahughes.hipr2.lower.image;
import net.joshuahughes.hipr2.lower.image1DDouble;
import net.joshuahughes.hipr2.lower.image1DInt;
import net.joshuahughes.hipr2.lower.image2DDouble;
import net.joshuahughes.hipr2.lower.image2DInt;
import net.joshuahughes.hipr2.upper.BinaryFast;
import net.joshuahughes.hipr2.upper.Log;
import net.joshuahughes.imagej.parameter.BooleanBoxParameter;
import net.joshuahughes.imagej.parameter.LabelParameter;
import net.joshuahughes.imagej.parameter.NumberSpinnerParameter;
import net.joshuahughes.imagej.parameter.Parameter;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.Type;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class HIPR2Plugin extends AbstractPlugin<Method>{
	//	public static LinkedHashSet<Class<?>> imageClassArray = new LinkedHashSet<Class<?>>(Arrays.<Class<?>>asList(int[].class,int[][].class,double[].class,double[][].class,BinaryFast.class,TwoImages.class,TwoDArray.class));
	public static LinkedHashSet<Class<?>> imageClassArray = new LinkedHashSet<Class<?>>(Arrays.<Class<?>>asList(int[].class,int[][].class,double[].class,double[][].class,BinaryFast.class));
	@Override
	protected String getName(Method t) {
		return t.getDeclaringClass().getSimpleName()+"."+t.getName();
	}
	@Override
	protected LinkedHashMap<Method,List<Parameter>> getMap() {
		LinkedHashMap<Method,List<Parameter>> map = new LinkedHashMap<Method,List<Parameter>>();
		List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
		classLoadersList.add(ClasspathHelper.contextClassLoader());
		classLoadersList.add(ClasspathHelper.staticClassLoader());

		Reflections reflections = new Reflections(new ConfigurationBuilder()
		.setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
		.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
		.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("net.joshuahughes.hipr2.upper"))));
		ArrayList<Class<? extends Object>> classList = new ArrayList<Class<? extends Object>>();
		classList.addAll(reflections.getSubTypesOf(Object.class));
		classList.addAll(reflections.getSubTypesOf(Thread.class));
		Collections.sort(classList, new Comparator<Class<?>>(){
			@Override
			public int compare(Class<?> arg0, Class<?> arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}
		});
		for(Class<? extends Object> clazz : classList){
			LinkedHashSet<Method> candidateSet = new LinkedHashSet<Method>();
			candidateSet.addAll(Arrays.asList(clazz.getMethods()));
			candidateSet.addAll(Arrays.asList(clazz.getDeclaredMethods()));
			for(Method method : candidateSet){
				if(isValid(method)){
					List<Parameter> parameterList = getParameterList(method);
					if(parameterList.size() == method.getParameterTypes().length-1 || method.getDeclaringClass().equals(Log.class))
						map.put(method,parameterList);
				}
			}
		}
		return map;
	}

	private boolean isValid(Method method) {
		if(Modifier.isPrivate(method.getModifiers())) return false;
		// TODO these methods may have a bug.
		if(method.getName().equals("distanceSingleIteration")) return false;
		if(method.getName().equals("apply_boundary"))return false;
		if(method.getName().equals("percentile_stretch"))return false;

		if(!imageClassArray.contains(method.getReturnType()))return false;
		if(method.getParameterTypes().length < 1) return false;
		if(!imageClassArray.contains(method.getParameterTypes()[0])) return false;
		// TODO eliminate the following condition and prepared to accept two images
		if(method.getParameterTypes().length > 1 && imageClassArray.contains(method.getParameterTypes()[1]))return false;
		return true;
	}
	private List<Parameter> getParameterList(Method method) {
		String zipName = method.getDeclaringClass().getName().replace('.', '/')+".class";
		ArrayList<String> nameList = new ArrayList<String>();
		try {
			JavaClass jc = new ClassParser("src/main/resources/hipr2.jar",zipName).parse();
			org.apache.bcel.classfile.Method bcelMethod = null;
			for(org.apache.bcel.classfile.Method m : jc.getMethods())
				if(same(m,method)){
					bcelMethod = m;
					String[] nameArray = bcelMethod.toString().substring(0,bcelMethod.toString().length()-1).split("\\(")[1].split(",");
					for(String name : nameArray)
						nameList.add(name.trim().split(" ")[1]);
					ArrayList<Parameter> list = new ArrayList<Parameter>();
					for(int ndx=1;ndx<method.getParameterTypes().length;ndx++){
						Class<?> pClass = method.getParameterTypes()[ndx];
						String name = nameList.get(ndx); 
						if(name.equals("width") || name.equals("w") || name.equals("height") || name.equals("h")){
							list.add(new LabelParameter(name,"not adjustable"));
							continue;
						}
						if(pClass.equals(long.class))
							list.add(new NumberSpinnerParameter<Long>(name,1l));
						if(pClass.equals(int.class))
							list.add(new NumberSpinnerParameter<Integer>(name,1));
						if(pClass.equals(double.class))
							list.add(new NumberSpinnerParameter<Double>(name,1d));
						if(pClass.equals(float.class))
							list.add(new NumberSpinnerParameter<Float>(name,1f));
						if(pClass.equals(boolean.class))
							list.add(new BooleanBoxParameter(name,true));
					}
					if(method.getDeclaringClass().equals(Log.class)){
						list.add(new NumberSpinnerParameter<Integer>("kernelSize",1));
						list.add(new NumberSpinnerParameter<Double>("theta",.01d));
					}
					return list;
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
	private boolean same(org.apache.bcel.classfile.Method m, Method method) {
		if(!m.getName().equals(method.getName())) return false;
		if(!m.getReturnType().equals(Type.getType(method.getReturnType()))) return false;
		if(m.getArgumentTypes().length != method.getParameterTypes().length) return false;
		for(int ndx=0;ndx<m.getArgumentTypes().length;ndx++)
			if(!m.getArgumentTypes()[ndx].equals(Type.getType(method.getParameterTypes()[ndx])))
				return false;
		return true;
	}
	@Override
	protected BufferedImage getImage(BufferedImage image, Method method,List<Parameter> list) {
		try {
			Object[] values = new Object[1+list.size()];
			int ndx=0;
			values[ndx++] = fromBufferedImage(image,method.getParameterTypes()[0]);
			for(Parameter parameter : list){
				Object value = parameter.getCurrentValue();
				if(parameter.getName().equals("width") || parameter.getName().equals("w"))
					value = image.getWidth();
				if(parameter.getName().equals("height") || parameter.getName().equals("h"))
					value = image.getHeight();
				values[ndx++] = value;				
			}
			Object instance = null;
			if(method.getDeclaringClass().equals(Log.class)){
				instance = new Log((Integer)values[values.length-2],(Double)values[values.length-1]);
				values = Arrays.copyOf(values, values.length-2);
			}else
				try{
					instance = method.getDeclaringClass().getConstructor(int.class,int.class).newInstance(image.getWidth(),image.getHeight());
				}catch(Exception e){
					try{
						instance = method.getDeclaringClass().getConstructor().newInstance();
					}catch(Exception e2){
						instance = method.getDeclaringClass().getConstructor(int.class).newInstance(image.getWidth());
					}
				}
//			Object object = mean_thresh((int[])values[0],(Integer)values[1],(Integer)values[2],(Integer)values[3],(Integer)values[4]);
			Object object = method.invoke(instance,values);
			BufferedImage bi = toBufferedImage(object,image.getWidth(),image.getHeight()); 
			return bi;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private Object fromBufferedImage(BufferedImage image, Class<?> firstParameter) {
		int[] result = new int[image.getWidth()*image.getHeight()];
		int iMax = Integer.MIN_VALUE;
		int iMin = Integer.MAX_VALUE;
		int index=0;
		for(int y=0;y<image.getHeight();y++)
			for(int x=0;x<image.getWidth();x++){
				Color color = new Color(image.getRGB(x, y));
				int i = (int)((color.getRed()+color.getGreen()+color.getBlue())/3d);
				iMax = Math.max(iMax,i);
				iMin = Math.min(iMin,i);
				result[index++] = i;
			}
		if(firstParameter.equals(int[].class)) return result;
		if(firstParameter.equals(int[][].class)) return new image2DInt(image.getWidth(),image.getHeight(),result).getValues2D();
		if(firstParameter.equals(double[].class)) return new image1DDouble(image.getWidth(),image.getHeight(),result).getValuesDouble(); 
		if(firstParameter.equals(double[][].class)) return new image2DDouble(image.getWidth(),image.getHeight(),result).getValues2D();
		if(firstParameter.equals(BinaryFast.class)) return new BinaryFast(new image1DInt(image.getWidth(),image.getHeight(),result));
		return null;
	}
	private BufferedImage toBufferedImage(Object hipr2Img,int width,int height) {
		BufferedImage result = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
		image img = null;
		if(hipr2Img instanceof int[])
			img = new image1DInt(width,height,(int[])hipr2Img);
		if(hipr2Img instanceof int[][])
			img = new image2DInt(width,height,(int[][])hipr2Img);
		if(hipr2Img instanceof double[])
			img = new image1DDouble(width,height,(double[])hipr2Img);
		if(hipr2Img instanceof double[][])
			img = new image2DDouble(width,height,(double[][])hipr2Img);
		if(hipr2Img instanceof BinaryFast)
			img = (BinaryFast) hipr2Img;
		if(img!=null){
			int[] imgValues = img.getValues();
			int index=0;
			for(int y=0;y<result.getHeight();y++)
				for(int x=0;x<result.getWidth();x++)
					result.setRGB(x, y,imgValues[index++]);
			return result;
		}
		result.createGraphics().drawString(hipr2Img.getClass().getSimpleName()+" not supported", 10, result.getHeight()/2);
		return result;
	}
	public static void main(String[] args){
		ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
		imp.show();
		new HIPR2Plugin().showDialog(imp,null,null);
	}
}
