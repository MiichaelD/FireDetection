package app;
//Importamos la libreria para el manejo de sentencias SQL.
import java.io.*;
import java.util.Date;
import java.sql.*;
//Declaracion de la clase.
public class Principal{
	//Declaracion de variables.
	Conexion conexion = new Conexion();
	Statement stm = null;
	PreparedStatement psa = null;
	ResultSet rs = null, rsa = null;
	String path=System.getProperty("user.home")+File.separator+"Desktop"+File.separator;
	int a=0, i=0, r=0; //Iteradores.
	int edo_1=0, edo_2 = 0, edo_3 = 0, edo_4=0, lim = 56, window = 55; //Estados de la maquina.
	float t_acum[] = new float[lim], l_acum[] = new float[lim], h_acum[] = new float[lim]; //Acumuladores de las mediciones.
	float t_sum = 0, l_sum = 0, h_sum = 0; //Sumatorias.
	float t_ratio = 0, l_ratio = 0, h_ratio = 0;  //Resultados de la ventana.
	float t_threshold = (float) 1.01, l_threshold = (float) 1.1, h_threshold = (float) 1, tm_threshold = 3; //Umbrales.
	float ini = 0, fin = 0, dif = 0, cont = 0, pico = 0;
	
	public void iteraciones(){
		conexion.conectar(); //Abrimos una conexion con la base de datos.
		try {
			psa = conexion.con.prepareStatement("SELECT * FROM MUESTRAS WHERE temperature != 1 and day(fechahora) = 26 and month(fechahora) = 8");
			//Procesamiento de la sentencia SQL.
			rsa = psa.executeQuery();
			//Comienza a determin(fechahora)ar la cantidad de registros.
			while(rsa.next()){
				r++;
			}
		} catch (SQLException e) {
			//Se devuelve algun tipo de error.
			e.printStackTrace();
		}
	}
	
	public void maquina() throws IOException{
		String arch = path+"Test_W_"+window+".txt";
        FileWriter fw = new FileWriter (arch);
        BufferedWriter bw = new BufferedWriter (fw);
        PrintWriter salArch = new PrintWriter (bw);
		iteraciones();//Se obtiene la cantidad a repetir el procedimiento.
		do{
			try {
				stm = conexion.con.createStatement(); //Se establece una conexion.
				rs = stm.executeQuery("SELECT * FROM MUESTRAS WHERE temperature != 1 and day(fechahora)=26 and month(fechahora) = 8 LIMIT "+a+","+lim); //Se crea la sentencia libre de espurios.
				//Comienza el metodo de la ventana.
				while(rs.next()){
					t_acum[i] = rs.getFloat("temperature");//Se almacenan la medicion del sensor de temperatura en la posicion i.
					l_acum[i] = rs.getInt("vis_light");//Se almacenan la medicion del sensor de luz en la posicion i.
					h_acum[i] = rs.getFloat("humidity");//Se almacenan la medicion del sensor de humedad en la posicion i.
					i++; //Se incrementa la posicion i.
					if(i>window){ //Cuando la posicion i sea mayor a 20 el arreglo esta completo.
						//Se calcula la sumatoria para cada tipo de medicion.
						for(int j=0; j<window; j++){
							t_sum = t_sum + t_acum[j];
							l_sum = l_sum + l_acum[j];
							h_sum = h_sum + h_acum[j];
						}
						//Se calculan los radios a evaluar con los umbrales definidos anteriormente.
						t_ratio = t_acum[window]/(t_sum/window);
						l_ratio = l_acum[window]/(l_sum/window);
						h_ratio = h_acum[window]/(h_sum/window);
						//Para cambiar de estado 0 a 2 el radio de temperatura y de luz deben ser mayores que los umbrales definidos anteriormente.
						if((t_ratio > t_threshold) && (l_ratio > l_threshold)){
							edo_2++;
							if(edo_2 >= 3){
								//Para cambiar de estado 2 a 4 el radio de humedad debe ser mayor al umbral definido anteriormente.
								if(h_ratio < h_threshold){
									edo_4++;
									//Para cambiar de estado 4 a 5 las mediciones deben superar los umbrales mas de tres veces.
									if(edo_4 >= 3){
										if((t_ratio > t_threshold) && (l_ratio > l_threshold)){
											salArch.println("Estado 2 = "+edo_2);
											salArch.println("Estado 4 = "+edo_4);
											salArch.println("Razon de cambio de temperatura: "+t_ratio);
											salArch.println("Razon de cambio de luz: "+l_ratio);
											salArch.println("Razon de cambio de humedad: "+h_ratio);
											//Se muestran los datos del incendio.
											salArch.println(" INCENDIO "+
															   " Fecha "+rs.getString("fechahora")+
														       " Temperatura "+rs.getFloat("temperature")+
														       " Luz "+rs.getInt("vis_light")+
														       " Humedad "+rs.getDouble("humidity"));	
											edo_2 = 0;
											edo_4 = 0;
										}
										else{
											edo_4--;
										}
									}	
								}
								//En caso contrario se descarta la posibilidad del cambio de estado.
								else{
									edo_2--;	
								}
							}
						}
						//Para cambiar de estado 0 a 1 el radio de temperatura y de luz deben ser mayores que los umbrales definidos anteriormente.
						else if((t_ratio > t_threshold) && (l_ratio <= l_threshold)){
							edo_1++;
							cont++;
							if(cont == 1){
								ini = rs.getFloat("temperature");
							}
				
							if(edo_1 >= 3){
								//Para cambiar de estado 1 a 3 el radio de humedad debe ser mayor al umbral definido anteriormente.
								if(h_ratio < h_threshold){
									edo_3++;
									
									//Para cambiar de estado 3 a 5 las mediciones deben superar los umbrales mas de tres veces.
									if(edo_3 >= 3){
										if((t_ratio > t_threshold) && (h_ratio < h_threshold)){
											if(cont >= 2){
												fin = rs.getFloat("temperature");
												if(fin > pico){
													pico = fin;
													
													dif = fin - ini;
													if(dif >= tm_threshold){
														salArch.println("Pico ="+pico);
														salArch.println("Estado 1 = "+edo_1);
														salArch.println("Estado 3 = "+edo_3);
														salArch.println("Inicial "+ini);
														salArch.println("Final "+fin);
														salArch.println("Diferencia = "+dif);
														salArch.println("Razon de cambio de temperatura: "+t_ratio);
														salArch.println("Razon de cambio de luz: "+l_ratio);
														salArch.println("Razon de cambio de humedad: "+h_ratio);
														//Se muestran los datos del incendio.
														salArch.println(" INCENDIO "+
																	" Fecha "+rs.getString("fechahora")+
															       " Temperatura "+rs.getFloat("temperature")+
															       " Luz "+rs.getInt("vis_light")+
															       " Humedad "+rs.getDouble("humidity"));	
														cont = 0;
														//pico = 0;
													}
													else{
														cont=1;
													}
												}
												else{
													cont = 0;
													pico = fin;
												}												
											}

											edo_1 = 0;
											edo_3 = 0;
										}
										else{
											edo_3--;
										}
									}
								}
								//En caso contrario se descarta la posibilidad del cambio de estado.
								else{
									edo_1--;
								}
							}
						}
						// Se limpian las variables correspondientes.
						t_ratio = 0;
						t_sum = 0;
						l_ratio = 0;
						l_sum = 0;
						h_ratio = 0;
						h_sum = 0;
						i=0;
					}
				}
			} catch (SQLException e) {
				// Devuelve algun tipo de error.
				e.printStackTrace();
			}
		a++;
		}while(a<=r);//Mientras el limite no traspase la cantidad de registros el proceso se repite.
		salArch.close();
	}
	
	public static void main(String args[]){
		Principal xd = new Principal();
		//Comienza a ejecutarse la maquina de estados.
		long time=System.currentTimeMillis();
		System.out.println("Empezado:\t "+new Date(time));
		try {
			xd.maquina();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Concluye la maquina de estados.
		time=System.currentTimeMillis();
		System.out.println("Terminado a las !!"+new Date(time));
	}
}
