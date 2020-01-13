package rest.pojo;

import java.util.List;

public abstract class RestNode extends RestObject {
	public abstract long getID();
	public abstract String getName();
	public abstract void setName(String name);
	public abstract RestNode getParent();
	public abstract boolean setParent(String parent);
	public abstract boolean setParent(RestNode parent);
	public abstract List<RestNode> getChildren();
	public abstract boolean addChild(String child);
	public abstract boolean addChild(RestNode child);
	public abstract String toJson();
	public abstract boolean equals(Object obj);
}
