import java.io.Serializable;
import java.util.ArrayList;

public class User implements  Serializable{
	

	private static final long serialVersionUID = 1L;
	
	private String name;
	public  String password;
	
	
	
	  public ArrayList <Log>logs = new  ArrayList<>(); 

	
	public ArrayList<Log> getLogs() {
		return logs;
	}

	public void setLogs(ArrayList<Log> logs) {
		this.logs = logs;
	}

	public User () {
		this.name = getName();
		this.password = getPassword();
	}
	private String getPassword() {
		
		return password;
	}

	public User(String name, String password) {
		this.name = name;
		this.password = password;
	
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
