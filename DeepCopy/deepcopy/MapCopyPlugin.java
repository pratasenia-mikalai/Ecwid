package deepcopy;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapCopyPlugin implements DeepCopyModulePlugin {

    @Override
    public boolean supports(Object src) {
        return Map.class.isAssignableFrom(src.getClass());
    }

    @Override
    public boolean instantiateCopyObjects(Object src, CopyObjectContext context, Consumer<Object> nestedObjectsInstantiation) throws Exception {
        Class<?> srcClass = src.getClass();
        Map<?, ?> srcMap = (Map<?, ?>) src;
        Map<?, ?> newMapInstance;
        if (SortedMap.class.isAssignableFrom(srcClass) && ((SortedMap<?, ?>)srcMap).comparator() != null) {
            newMapInstance = (SortedMap<?, ?>) srcClass.getConstructor(Comparator.class).newInstance(((SortedMap<?, ?>)srcMap).comparator());
        } else {
            newMapInstance = (Map<?, ?>) srcClass.getConstructor().newInstance();
        }
        context.putCopyInstanceFor(srcMap, newMapInstance);
        context.registerValueDependentDataStructure(srcMap);
        for (Map.Entry<?, ?> entry : srcMap.entrySet()) {
            nestedObjectsInstantiation.accept(entry.getKey());
            nestedObjectsInstantiation.accept(entry.getValue());
        }
        return true;
    }

    @Override
    public boolean fillValueDependentDataStructure(Object src, CopyObjectContext context) {
        Map<?, ?> srcMap = (Map<?, ?>) src;
        Map<Object, Object> copyMap = (Map<Object, Object>) context.getCopyInstanceFor(srcMap);
        for (Map.Entry<?, ?> entry : srcMap.entrySet()) {
            copyMap.put(
                    context.getCopyInstanceFor(entry.getKey()),
                    context.getCopyInstanceFor(entry.getValue())
            );
        }
        return true;
    }
}
