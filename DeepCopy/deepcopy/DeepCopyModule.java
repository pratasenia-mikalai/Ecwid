package deepcopy;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class DeepCopyModule {

    private final Set<String> wrapperClassNames;
    private final List<Predicate<Class<?>>> wrapperClassPredicates;
    private final Map<String, UnaryOperator<?>> customCopyFunctions;
    private final Map<String, Supplier<?>> classInstanceSuppliers;
    private final List<DeepCopyModulePlugin> copyPlugins;

    private DeepCopyModule(Set<String> wrapperClassNames, List<Predicate<Class<?>>> wrapperClassPredicates, Map<String, UnaryOperator<?>> customCopyFunctions, Map<String, Supplier<?>> classInstanceSuppliers, List<DeepCopyModulePlugin> copyPlugins) {
        this.wrapperClassNames = wrapperClassNames;
        this.wrapperClassPredicates = wrapperClassPredicates;
        this.customCopyFunctions = customCopyFunctions;
        this.classInstanceSuppliers = classInstanceSuppliers;
        this.copyPlugins = copyPlugins;
    }

    public static DeepCopyModule.Builder builder() {
        return new Builder();
    }

    public <T> T deepCopy(T src) throws Exception {
        if (src == null) {
            return null;
        }
        CopyObjectContext context = new CopyObjectContext();

        instantiateCopyToContext(src, context);
        setReferenceValues(context);
        fillValueDependentDataStructures(context);

        return (T) context.getCopyInstanceFor(src);
    }

    private boolean isWrapper(Class<?> type) {
        return wrapperClassNames.contains(type.getName()) || wrapperClassPredicates.stream().anyMatch(wp -> wp.test(type));
    }

    private <T> UnaryOperator<T> customCopyFunction(Class<T> srcClass) {
        return (UnaryOperator<T>) customCopyFunctions.get(srcClass.getName());
    }

    private <T> void instantiateCopyToContext(T src, CopyObjectContext context) throws Exception {
        if (src == null || context.exists(src)) {
            return;
        }

        Class<T> srcClass = (Class<T>) src.getClass();

        var copyFunction = customCopyFunction(srcClass);
        if (copyFunction != null) {
            T newObject = copyFunction.apply(src);
            context.putCopyInstanceFor(src, newObject);
            return;
        }

        if (srcClass.isArray() && srcClass.getComponentType().isPrimitive()) {
            int length = Array.getLength(src);

            Object newArray = Array.newInstance(srcClass.getComponentType(), length);
            System.arraycopy(src, 0, newArray, 0, length);

            context.putCopyInstanceFor(src, newArray);
            return;
        }

        if (srcClass.isArray()) {
            Object[] srcAsArray = (Object[]) src;
            Object newArray = Array.newInstance(srcClass.getComponentType(), srcAsArray.length);
            context.putCopyInstanceFor(src, newArray);

            for (Object object : srcAsArray) {
                instantiateCopyToContext(object, context);
            }
            return;
        }

        if (isWrapper(srcClass)) {
            context.putCopyInstanceFor(src, src);
            return;
        }

        for (DeepCopyModulePlugin plugin : copyPlugins) {
            if (plugin.supports(src)) {
                boolean proceed = plugin.instantiateCopyObjects(src, context, (Object object) -> {
                    try {
                        this.instantiateCopyToContext(object, context);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                if (proceed) {
                    return;
                }
            }
        }

        T newObject = objectInstance(srcClass);
        context.putCopyInstanceFor(src, newObject);

        for (Field field : new FieldsWithValue(src)) {

            if (field.getType().isPrimitive()) {
                field.set(newObject, field.get(src));
                continue;
            }
            instantiateCopyToContext(field.get(src), context);
        }

    }

    private <T> T objectInstance(Class<T> cls) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (classInstanceSuppliers.containsKey(cls.getName())) {
            return (T) classInstanceSuppliers.get(cls.getName()).get();
        }

        try {
            Constructor<T> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            Constructor<T> constructor = (Constructor<T>) cls.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Class<?>[] paramClasses = constructor.getParameterTypes();
            Object[] params = new Object[paramClasses.length];
            for (int i = 0; i < paramClasses.length; i++) {
                if (paramClasses[i].isPrimitive()) {
                    if (paramClasses[i].equals(Boolean.TYPE)) params[i] = false;
                    else params[i] = (byte) 0b0;
                } else {
                    params[i] = null;
                }
            }
            return constructor.newInstance(params);
        }
    }

    private void setReferenceValues(CopyObjectContext context) throws IllegalAccessException {
        List<Object> srcObjectsList = context.srcObjectsAsList();

        for (Object srcObject : srcObjectsList) {
            Class<?> objectClass = srcObject.getClass();
            if (customCopyFunction(objectClass) != null) {
                continue;
            }
            if (isWrapper(objectClass) || (objectClass.isArray() && objectClass.getComponentType().isPrimitive())) {
                continue;
            }
            if (objectClass.isArray()) {
                Object[] srcAsArray = (Object[]) srcObject;
                Object[] newArray = (Object[]) context.getCopyInstanceFor(srcAsArray);
                for (int i = 0; i < srcAsArray.length; i++) {
                    newArray[i] = context.getCopyInstanceFor(srcAsArray[i]);
                }
                continue;
            }

            boolean proceedByPlugin = false;
            for (DeepCopyModulePlugin plugin : copyPlugins) {
                if (plugin.supports(srcObject)) {
                    proceedByPlugin = plugin.setReferenceFields(srcObject, context);
                    if (proceedByPlugin) {
                        break;
                    }
                }
            }
            if (proceedByPlugin) {
                continue;
            }

            for (Field field : new FieldsWithValue(srcObject)) {
                if (field.getType().isPrimitive()) {
                    continue;
                }
                Object copyObject = context.getCopyInstanceFor(srcObject);
                field.set(copyObject, context.getCopyInstanceFor(field.get(srcObject)));
            }
        }
    }

    private void fillValueDependentDataStructures(CopyObjectContext context) {
        // As structures are put from the highest level of object to the deepest one
        // data structures should be filled from end to start of list.
        // In case of using them in hashCode(), equals() and compare() calculation for higher level structures.
        List<Object> srcObjectsList = context.srcValueDependentDataStructures().reversed();

        for (Object srcDataStructure : srcObjectsList) {
            Class<?> structureClass = srcDataStructure.getClass();
            if (customCopyFunction(structureClass) != null) {
                continue;
            }

            for (DeepCopyModulePlugin plugin : copyPlugins) {
                if (plugin.supports(srcDataStructure)) {
                    boolean proceed = plugin.fillValueDependentDataStructure(srcDataStructure, context);
                    if (proceed) {
                        break;
                    }
                }
            }
        }
    }

    public static class Builder {

        private final Set<String> wrapperClassNames = new HashSet<>();
        private final List<Predicate<Class<?>>> wrapperClassPredicates = new ArrayList<>();
        private final Map<String, UnaryOperator<?>> customCopyFunctions = new HashMap<>();
        private final Map<String, Supplier<?>> classInstanceSuppliers = new HashMap<>();
        private final List<PluginWithPriority> copyPlugins = new ArrayList<>();

        private Builder() {
            registerWrapperClass(Boolean.class);
            registerWrapperClass(Byte.class);
            registerWrapperClass(Short.class);
            registerWrapperClass(Integer.class);
            registerWrapperClass(Long.class);
            registerWrapperClass(Float.class);
            registerWrapperClass(Double.class);
            registerWrapperClass(Character.class);
            registerWrapperClass(String.class);
            registerWrapperClassPredicate(Class::isEnum);
            registerWrapperClassPredicate(cls -> cls.getPackageName().equals("java.time"));
            registerWrapperClassPredicate(cls -> Number.class.equals(cls.getSuperclass()) && !cls.getPackageName().equals("java.util.concurrent.atomic"));

            registerCopyPlugin(new ListCopyPlugin(), Integer.MAX_VALUE);
            registerCopyPlugin(new SetCopyPlugin(), Integer.MAX_VALUE);
            registerCopyPlugin(new MapCopyPlugin(), Integer.MAX_VALUE);
        }

        public DeepCopyModule build() {
            return new DeepCopyModule(
                    this.wrapperClassNames,
                    this.wrapperClassPredicates,
                    this.customCopyFunctions,
                    this.classInstanceSuppliers,
                    this.copyPlugins.stream()
                            .sorted(Comparator.comparing(PluginWithPriority::priority))
                            .map(PluginWithPriority::plugin)
                            .toList()
            );
        }

        public Builder registerWrapperClass(Class<?> wrapperClass) {
            this.wrapperClassNames.add(wrapperClass.getName());
            return this;
        }

        public Builder registerWrapperClassPredicate(Predicate<Class<?>> wrapperPredicate) {
            this.wrapperClassPredicates.add(wrapperPredicate);
            return this;
        }

        public <T> Builder registerClassInstanceSupplier(Class<T> instantiableClass, Supplier<T> instanceSupplier) {
            this.classInstanceSuppliers.put(instantiableClass.getName(), instanceSupplier);
            return this;
        }

        public <T> Builder registerCustomCopyFunction(Class<T> copiableObjectClass, UnaryOperator<T> copyFunction) {
            this.customCopyFunctions.put(copiableObjectClass.getName(), copyFunction);
            return this;
        }

        public Builder registerCopyPlugin(DeepCopyModulePlugin plugin, int priority) {
            this.copyPlugins.add(new PluginWithPriority(priority, plugin));
            return this;
        }

        private record PluginWithPriority(
             int priority,
             DeepCopyModulePlugin plugin
        ) {}
    }

}
