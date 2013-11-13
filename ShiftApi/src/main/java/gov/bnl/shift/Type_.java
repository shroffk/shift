package gov.bnl.shift;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Type.class)
public class Type_ {
    public static volatile SingularAttribute<Type, Integer> id;
    public static volatile SingularAttribute<Type, String> name;

}
