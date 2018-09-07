package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class UMLTypeParameter {
	private String name;
	private List<UMLType> typeBounds;

	public UMLTypeParameter(String name) {
		this.name = name;
		this.typeBounds = new ArrayList<UMLType>();
	}
	
	public void addTypeBound(UMLType type) {
		typeBounds.add(type);
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLTypeParameter other = (UMLTypeParameter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
