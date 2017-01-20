package be.grgt.jonas.bob;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public abstract class BobTheBuilder<T> implements Builder<T> {

    protected T instance;

    public BobTheBuilder() {
        instance = newInstance();
    }

    abstract protected T newInstance();

    protected void setField(String name, Object value) {
        try {
            Field field = instance.getClass().
                    getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T get() {
        T result = instance;
        instance = newInstance();
        return result;
    }
}
