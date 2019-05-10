import java.io.Serializable;
import java.util.Date;

public class Log implements Serializable{

	private static final long serialVersionUID = 1L;
	private Date date;
	private String name;
	private int diveNo;
	private String location;
	private int airTemp;
	private int waterTemp;
	private int tankStart;
	private int tankEnd;
	private int weight;

	private String comments;
	//dive profile spinner input
	private int surfaceIntervalHrs;
	private int surfaceIntervalMin;
	private int depth;
	private int bottomTime;

	private String pressureGroup; // for actual dive
	private String pressureGroup2; // for dive plan # 2
	private String residualNitrogenTime;
	private String totalBottomTime;
	// for Dive type check boxes
	private boolean fresh;
	private boolean salt;
	private boolean shore;
	private boolean boat;
	private boolean waves;
	private boolean current;
	private boolean surge;
	// for exposure protection check boxes
	private boolean none;
	private boolean wetSuit;
	private boolean drySuit;
	private boolean hood;
	private boolean shorty;
	private boolean gloves;
	private boolean boots;

	public Log() {

	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDiveNo() {
		return diveNo;
	}

	public void setDiveNo(int diveNo) {
		this.diveNo = diveNo;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getAirTemp() {
		return airTemp;
	}

	public void setAirTemp(int airTemp) {
		this.airTemp = airTemp;
	}

	public int getWaterTemp() {
		return waterTemp;
	}

	public void setWaterTemp(int waterTemp) {
		this.waterTemp = waterTemp;
	}

	public int getTankStart() {
		return tankStart;
	}

	public void setTankStart(int tankStart) {
		this.tankStart = tankStart;
	}

	public int getTankEnd() {
		return tankEnd;
	}

	public void setTankEnd(int tankEnd) {
		this.tankEnd = tankEnd;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public int getSurfaceIntervalHrs() {
		return surfaceIntervalHrs;
	}

	public void setSurfaceIntervalHrs(int surfaceIntervalHrs) {
		this.surfaceIntervalHrs = surfaceIntervalHrs;
	}

	public int getSurfaceIntervalMin() {
		return surfaceIntervalMin;
	}

	public void setSurfaceIntervalMin(int surfaceIntervalMin) {
		this.surfaceIntervalMin = surfaceIntervalMin;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getBottomTime() {
		return bottomTime;
	}

	public void setBottomTime(int bottomTime) {
		this.bottomTime = bottomTime;
	}

	public String getPressureGroup() {
		return pressureGroup;
	}

	public void setPressureGroup(String pressureGroup) {
		this.pressureGroup = pressureGroup;
	}

	public String getPressureGroup2() {
		return pressureGroup2;
	}

	public void setPressureGroup2(String pressureGroup2) {
		this.pressureGroup2 = pressureGroup2;
	}

	public boolean isFresh() {
		return fresh;
	}

	public void setFresh(boolean fresh) {
		this.fresh = fresh;
	}

	public boolean isSalt() {
		return salt;
	}

	public void setSalt(boolean salt) {
		this.salt = salt;
	}

	public boolean isShore() {
		return shore;
	}

	public void setShore(boolean shore) {
		this.shore = shore;
	}

	public boolean isBoat() {
		return boat;
	}

	public void setBoat(boolean boat) {
		this.boat = boat;
	}

	public boolean isWaves() {
		return waves;
	}

	public void setWaves(boolean waves) {
		this.waves = waves;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}





	public boolean isSurge() {
		return surge;
	}

	public void setSurge(boolean surge) {
		this.surge = surge;
	}

	public boolean isNone() {
		return none;
	}

	public void setNone(boolean none) {
		this.none = none;
	}

	public boolean isWetSuit() {
		return wetSuit;
	}

	public void setWetSuit(boolean wetSuit) {
		this.wetSuit = wetSuit;
	}

	public boolean isDrySuit() {
		return drySuit;
	}

	public void setDrySuit(boolean drySuit) {
		this.drySuit = drySuit;
	}

	public boolean isHood() {
		return hood;
	}

	public void setHood(boolean hood) {
		this.hood = hood;
	}

	public boolean isShorty() {
		return shorty;
	}

	public void setShorty(boolean shorty) {
		this.shorty = shorty;
	}

	public boolean isGloves() {
		return gloves;
	}

	public void setGloves(boolean gloves) {
		this.gloves = gloves;
	}

	public boolean isBoots() {
		return boots;
	}

	public void setBoots(boolean boots) {
		this.boots = boots;
	}
	public String getResidualNitrogenTime() {
		return residualNitrogenTime;
	}

	public void setResidualNitrogenTime(String residualNitrogenTime) {
		this.residualNitrogenTime = residualNitrogenTime;
	}
	
	public String getTotalBottomTime() {
		return totalBottomTime;
	}

	public void setTotalBottomTime(String totalBottomTime) {
		this.totalBottomTime = totalBottomTime;
	}

	@Override
	public String toString() {
		
		return "\n\n---------------------------------------------------------------"
				+ "\nName :"+ name+"\nDate :"+date +"\nDive number :"+ diveNo+"\nLocation :"+location+"\nAir temp :"+ airTemp+
				"\nWater temp :"+ waterTemp + "\nTank PSI start :"+ tankStart + "\nTank PSI  end :"+ tankEnd+
				"\nWeight :"+ weight+ "\nComents :"+ comments+ "\nIs Fresh :"+ fresh+"\nIs Salt :"+salt+
				"\nIs Shore :"+ shore+"\nIs Boat :"+ boat+"\nIs Waves :"+ waves+ "\nIs Current :"+ current+
				"\nIs Surge :"+ surge+ "\nIs None :"+ none+ "\nIs WetSuit :"+ wetSuit+
				"\nIs DrySuit :"+ drySuit+"\nIs Hood :"+ hood+"\nIs Shorty :"+ shorty+"\nIs Gloves :"+ gloves+ 
				"\nIs Boots :"+ boots + "\n"+ "\nSurface Interval hrs : "+ surfaceIntervalHrs+ "\nSurface Interval min : "+surfaceIntervalMin+
				"\nDepth : "+ depth+"\nBottom Time : "+ bottomTime+ "\nPressure Group Starting  : "+ pressureGroup+ "\nPressure Group Ending : "+
				pressureGroup2+ "\nResidual Nitrogen Time : "+ residualNitrogenTime +"\nActual Bottom Time : "+bottomTime+
				"\nTotal Bottom Time : "+ totalBottomTime +"\n-----------------------------------------------------------------------------\n" ;
	}
}
