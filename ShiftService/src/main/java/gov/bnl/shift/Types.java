package gov.bnl.shift;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.LinkedList;
import java.util.List;

/**
 *@author :eschuhmacher
 */
@XmlRootElement(name = "types")
public class Types extends LinkedList<Type> {

    public Types() {
    }

    public Types(Type type) {
        this.add(type);
    }

    public Types(List<Type> types) {
        this.addAll(types);
    }

    @XmlElementRef(type = Type.class, name = "type")
    public List<Type> getTypeList() {
        return this;
    }

    @XmlTransient
    public List<Type> getTypes() {
        return this;
    }

    public void setTypes(List<Type> types) {
        this.addAll(types);
    }

    public void addType(Type type) {
        this.add(type);
    }
}
