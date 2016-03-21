package app;
import java.sql.*;

public class Conexion {
	//Elementos para establecer la conexión de JAVA con MYSQL.
	static String bd = "bdmts400"; 
	static String login = "root";
	static String password = "12qweasz";
	static String url = "jdbc:mysql://localhost/"+bd;
	Connection con = null;
	
	public Connection getConnection(){
		return con;
	}
	
	public void conectar(){
		//Se construye la conexión de datos.
		try{
			Class.forName("com.mysql.jdbc.Driver");
			//Obtenemos la conexión.
				con = DriverManager.getConnection(url,login,password); 
				if(con!=null){
					System.out.println("Conexi�n a base de datos "+bd+" OK");
				}
		}
		catch (SQLException e) {
			System.out.println(e);
		}
		catch(ClassNotFoundException e){
			System.out.println(e);
		}
	}
	
	public void desconectar(){
		con = null;
	}

}
