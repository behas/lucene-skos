package at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes;

import org.apache.lucene.util.AttributeImpl;

/**
 * Implementation class for SKOS-specific attributes
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 *
 */
public class SKOSAttributeImpl extends AttributeImpl implements SKOSAttribute {

	private static final long serialVersionUID = -6358866223731185045L;
	
	private SKOSType st;
	
	
	public void setSKOSType(SKOSType st) {
		this.st = st;
	}

	public SKOSType getSKOSType() {
		return this.st;
	}

	@Override
	public void clear() {
		st = null;
	}

	@Override
	public void copyTo(AttributeImpl other) {
		((SKOSAttribute) other).setSKOSType(st);
	}

	
	@Override
	public String toString() {
		return st.toString();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((st == null) ? 0 : st.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SKOSAttributeImpl other = (SKOSAttributeImpl) obj;
		if (st != other.st)
			return false;
		return true;
	}




	
	
	
}
