package deepcopy;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListCopyPlugin implements DeepCopyModulePlugin {

    @Override
    public boolean supports(Object src) {
        return List.class.isAssignableFrom(src.getClass());
    }

    @Override
    public boolean instantiateCopyObjects(Object src, CopyObjectContext context, Consumer<Object> nestedObjectsInstantiation) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<?> srcList = (List<?>) src;
        Object newListInstance = srcList.getClass().getConstructor().newInstance();
        context.putCopyInstanceFor(src, newListInstance);
        for (Object value : srcList) {
            nestedObjectsInstantiation.accept(value);
        }
        return true;
    }

    @Override
    public boolean setReferenceFields(Object src, CopyObjectContext context) {
        List<?> srcList = (List<?>) src;
        List<Object> copyList = (List<Object>) context.getCopyInstanceFor(srcList);
        srcList.forEach(it -> copyList.add(context.getCopyInstanceFor(it)));
        return true;
    }

}
