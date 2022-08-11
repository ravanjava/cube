package com.gap.pem.cds;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds a set of LevelMembers for one or more dimensions.  Each LevelMember must be from a different Dimension.
 */
public class LevelMemberTuple implements Serializable 
{
	private static final long serialVersionUID = 514769246185531L;
	
	protected HashSet<LevelMember> levelMembers;

	public LevelMemberTuple(LevelMember member) {
		if(member == null){
			throw new CdsException("LevelMemberTuple.ctor(): LevelMember cannot be null");
		}
		levelMembers = new HashSet<LevelMember>();
		levelMembers.add(member);
	}

	public LevelMemberTuple( Collection<LevelMember>  members ) {
		// Ensure that every level member comes from a different dimension
		HashSet<String> dimensionNames = new HashSet<String>();
		for( LevelMember member : members ) {
			if(member == null){
				throw new CdsException("LevelMemberTuple.ctor(): LevelMember cannot be null");
			}
			String memberDimension = member.getLevel().getDimensionName();
			if ( dimensionNames.contains( memberDimension ) )
				throw new CdsException("LevelMemberTuple.ctor(): Each LevelMember must come from a different Dimension");
			dimensionNames.add( memberDimension );
		}
		levelMembers = new HashSet<LevelMember>();
		levelMembers.addAll( members );
	}
	
	public Set<LevelMember> getLevelMembers() {
		return levelMembers;
	}
	
	/**
	 * Get the level and member at the given dimension.
	 * 
	 * @param dimensionName name of a dimension
	 * @return a level member or null.
	 */
	public LevelMember getLevelMember(String dimensionName){
		LevelMember result = null;
		for(LevelMember member : levelMembers) {
			String memberDimension = member.getLevel().getDimensionName();
			if(memberDimension.equals(dimensionName)){
				result = member;
				break;
			}
		}
		return result;
	}

	// Two level member tuples are equal if they have the same set of
	// level members.
	@Override
	public boolean equals(Object object) {
		if(object == this){
			return true;
		}
		
		if((object instanceof LevelMemberTuple) == false){
			return false;
		}
		
		LevelMemberTuple other = (LevelMemberTuple)object;
		return (other.levelMembers.equals(this.levelMembers));
	}
	
	@Override
	public int hashCode(){
		return this.levelMembers.hashCode();
	}
	
	@Override
	public String toString() {
		return this.levelMembers.toString();
	}
}

