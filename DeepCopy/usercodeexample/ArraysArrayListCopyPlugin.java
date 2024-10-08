package usercodeexample;

import deepcopy.CopyObjectContext;
import deepcopy.DeepCopyModulePlugin;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Example of custom implementation of plugin for copying of Arrays.ArrayList that is not support add() method.
 */
public class ArraysArrayListCopyPlugin implements DeepCopyModulePlugin {

    private final Class<?> supportedClass = Arrays.asList().getClass();

    @Override
    public boolean supports(Object src) {
        return src.getClass().equals(supportedClass);
    }

    @Override
    public boolean instantiateCopyObjects(Object src, CopyObjectContext context, Consumer<Object> nestedObjectsInstantiation) throws Exception {
        List<?> srcList = (List<?>) src;
        List<Object> copyList = Arrays.asList(srcList.toArray());
        context.putCopyInstanceFor(srcList, copyList);
        for (Object srcElement: srcList) {
            nestedObjectsInstantiation.accept(srcElement);
        }
        return true;
    }

    @Override
    public boolean setReferenceFields(Object src, CopyObjectContext context) {
        List<?> srcList = (List<?>) src;
        List<Object> copyList = (List<Object>) context.getCopyInstanceFor(srcList);
        for (int i = 0; i < srcList.size(); i++) {
            copyList.set(i, context.getCopyInstanceFor(srcList.get(i)));
        }
        return true;
    }

}
