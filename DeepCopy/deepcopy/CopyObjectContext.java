package deepcopy;

import java.util.*;

public class CopyObjectContext {

    private Map<String, Object> srcRefToCopyObjectMap = new HashMap<>();
    private List<Object> srcObjects = new ArrayList<>();
    private List<Object> valueDependentDataStructures = new ArrayList<>();

    public void putCopyInstanceFor(Object proto, Object copy) {
        this.srcRefToCopyObjectMap.put(refString(proto), copy);
        this.srcObjects.add(proto);
    }

    public Object getCopyInstanceFor(Object proto) {
        return this.srcRefToCopyObjectMap.get(refString(proto));
    }

    public void registerValueDependentDataStructure(Object dataStructure) {
        valueDependentDataStructures.add(dataStructure);
    }

    public boolean exists(Object proto) {
        return this.srcRefToCopyObjectMap.containsKey(refString(proto));
    }

    public List<Object> srcObjectsAsList() {
        return srcObjects;
    }

    public List<Object> srcValueDependentDataStructures() {
        return valueDependentDataStructures;
    }

    private String refString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

}
