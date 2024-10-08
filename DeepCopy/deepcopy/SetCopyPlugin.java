package deepcopy;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

public class SetCopyPlugin implements DeepCopyModulePlugin {

    @Override
    public boolean supports(Object src) {
        return Set.class.isAssignableFrom(src.getClass());
    }

    @Override
    public boolean instantiateCopyObjects(Object src, CopyObjectContext context, Consumer<Object> nestedObjectsInstantiation) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> srcClass = src.getClass();
        Set<?> srcSet = (Set<?>) src;
        Set<?> newSetInstance;
        if (SortedSet.class.isAssignableFrom(srcClass) && ((SortedSet<?>)srcSet).comparator() != null) {
            newSetInstance = (SortedSet<?>) srcClass.getConstructor(Comparator.class).newInstance(((SortedSet<?>)srcSet).comparator());
        } else {
            newSetInstance = (Set<?>) srcClass.getConstructor().newInstance();
        }
        context.putCopyInstanceFor(srcSet, newSetInstance);
        context.registerValueDependentDataStructure(srcSet);
        for (Object value : srcSet) {
            nestedObjectsInstantiation.accept(value);
        }
        return true;
    }

    @Override
    public boolean fillValueDependentDataStructure(Object src, CopyObjectContext context) {
        Set<?> srcSet = (Set<?>) src;
        Set<Object> copySet = (Set<Object>) context.getCopyInstanceFor(srcSet);
        srcSet.forEach(it -> copySet.add(context.getCopyInstanceFor(it)));
        return true;
    }
}
