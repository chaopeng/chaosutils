package me.chaopeng.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * spring上下文
 * @author chao
 */
public class SpringContext implements BeanFactoryAware {

	private static BeanFactory BEAN_FACTORY = null;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		BEAN_FACTORY = beanFactory;
	}

	public static BeanFactory getBeanFactory(){
		return BEAN_FACTORY;
	}
	
	public static <T> T getBean(Class<T> cls){
		if(BEAN_FACTORY != null){
			return BEAN_FACTORY.getBean(cls);
		}
		return null;
	}
	
	public static Object getBean(String name){
		if(BEAN_FACTORY != null ){
			return BEAN_FACTORY.getBean(name);
		}
		return null;
		
	}
}
