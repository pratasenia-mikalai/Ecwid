package deepcopy;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DeepCopyModulePlugin {

    boolean supports(Object src);

    boolean instantiateCopyObjects(Object src, CopyObjectContext context, Consumer<Object> nestedObjectsInstantiation) throws Exception;

    default boolean setReferenceFields(Object src, CopyObjectContext context) {
        return true;
    }

    default boolean fillValueDependentDataStructure(Object src, CopyObjectContext context) {
        return true;
    }

}
