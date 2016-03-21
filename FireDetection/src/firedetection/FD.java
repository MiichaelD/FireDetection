package firedetection;

import java.sql.*;
import javax.swing.*;
import java.util.*;

public class FD {

    //final double tr_threshold=1.01,tl_threshold=1.1,tm_threshold=3,th_threshold=1;
    final double tr_threshold = 1.01, tl_threshold = 1.1, tm_threshold = 2, th_threshold = 1;
    String host, usuario, contra;
    boolean error;
    Connection cone;
    Statement St;
    ResultSet Rs;
    ArrayList<Registro> RegList;
    ArrayList<String> Detections;
    double[] avg, sum;
    int windowSize = 5, FirstIndex, LastIndex = windowSize, detections;
    boolean found = false;

    public static void main(String[] args) {
        FD f;
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        if (args.length == 0) {
            f = new FD();
            f.cargarBD("select * from muestras where date(fechahora)='2011-08-26';");
            f.threshold();
        } else {
            for (int i = 0; i < args.length; i++) {
                f = new FD();
                f.cargarBD(args[i]);
                f.threshold();
            }
        }
    }

    public FD() {
        host = "localhost";
        usuario = "itm";
        contra = "itmexicali";
        init();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            cone = DriverManager.getConnection("jdbc:mysql://" + host + "/" + "bdmts400" + "?user=" + "itm" + "&password=" + "itmexicali");
            St = cone.createStatement();
            Rs = St.executeQuery("select 1;");
        } catch (Exception e) {
            CambiarDatos();
        }

    }

    public void init() {
        detections = 0;
        FirstIndex = 0;
        RegList = new ArrayList();
        Detections = new ArrayList();
        avg = new double[3];
        sum = new double[3];

    }

    /*
select * from muestras where date(fechahora)>='2011-08-25';
Termino metodo Threshold en: 1948 con 0 detecciones
Termino metodo Threshold1 en: 2621 con 4 detecciones
Termino metodo Threshold1 en: 17457 con 12 detecciones
     */
    public void threshold() {
        long time = System.currentTimeMillis();
        while (LastIndex < RegList.size()) {
            if (found) {
                if (RegList.get(LastIndex).temp < avg[0]) {
                    found = false;
                }
            } else {
                if (state0()) {//checa avgTemp > threshold
                    if (state1()) {//checa iluminacion > prom
                        if (state23()) {//checa humedad
                            Detections.add("Incendio Detectado!! a la fecha: " + RegList.get(LastIndex).fecha);
                            found = true;
                        }
                    } else { //iluminacion <=
                        if (state23()) {//checa humedad
                            if (state4()) {//TempFin-TempIni > 3grados
                                Detections.add("Incendio Detectado!! a la fecha: " + RegList.get(LastIndex).fecha);
                                found = true;
                            }
                        }
                    }
                }
            }
            updateAvg();
            FirstIndex++;
            LastIndex++;
        }
        System.out.printf("Termino metodo Threshold en: %d\nIncendios detectados: %d\n",
                (System.currentTimeMillis() - time),Detections.size());
/*        System.out.println("Termino metodo Threshold en "+(System.currentTimeMillis() - time));
        System.out.println("Incendios Detectados: "+Detections.size());
  */      for (int i = 0; i < Detections.size(); i++) {
 //           System.out.println(Detections.get(i));
            System.out.printf("%s\n",Detections.get(i));
        }
    }

    public boolean state0() {
        if ((RegList.get(LastIndex).temp / avg[0]) > tr_threshold) {
            return true;
        }
        return false;
    }

    public boolean state1() {
        if ((RegList.get(LastIndex).vis / avg[1]) > tl_threshold) {
            return true;
        }
        return false;
    }

    public boolean state23() {
        //RegList.get(LastIndex).hum/avg[2]
        if ((avg[2] / RegList.get(LastIndex).hum) > th_threshold) {
            return true;
        }
        return false;
    }

    public boolean state4() {
        if ((RegList.get(LastIndex).temp - RegList.get(FirstIndex).temp) >= tm_threshold) {
            return true;
        }
        return false;
    }

    public void cargarBD(String query) {
        try {
            coneccionBD("select", query);
            while (Rs.next()) {
                RegList.add(new Registro(Rs.getString("fechahora"), Rs.getDouble("temperature"), Rs.getDouble("humidity"), Rs.getInt("vis_light")));
                /*            System.out.println(Rs.getTimestamp("fechahora")+"\t"+Rs.getDouble("temperature")+"\t\t"+Rs.getDouble("humidity")
                +"\t\t"+Rs.getInt("vis_light")+"\t\t"+Rs.getInt("id_node") +"\t");
                 */            }
            avg();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void avg() { //parametros: 0=temperatura, 1=vis_light, 2=humedad
        for (int i = FirstIndex; i < LastIndex; i++) {
            sum[0] += RegList.get(i).temp;
            sum[1] += RegList.get(i).vis;
            sum[2] += RegList.get(i).hum;
        }
        for (int i = 0; i < 3; i++) {
            avg[i] = redondear(sum[i] / windowSize, 2);
        }
    }

    public void updateAvg() {
        sum[0] = sum[0] + RegList.get(LastIndex).temp - RegList.get(FirstIndex).temp;
        sum[1] = sum[1] + RegList.get(LastIndex).vis - RegList.get(FirstIndex).vis;
        sum[2] = sum[2] + RegList.get(LastIndex).hum - RegList.get(FirstIndex).hum;
        for (int i = 0; i < 3; i++) {
            avg[i] = redondear(sum[i] / windowSize, 2);
        }
    }

    public double redondear(double doble, int digitos) {
        int cifras = (int) Math.pow(10, digitos);
        return Math.rint(doble * cifras) / cifras;
    }

    public void CambiarDatos() {
        host = JOptionPane.showInputDialog("Localizacion del Host", host);
        usuario = JOptionPane.showInputDialog("Usuario MySQL", host);
        contra = JOptionPane.showInputDialog("Contraseña MySQL", host);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            cone = DriverManager.getConnection("jdbc:mysql://" + host + "/" + "bdmts400" + "?user=" + usuario + "&password=" + contra);
            St = cone.createStatement();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e + "\nen CambiarDatos");
        }
    }

    public void coneccionBD(String s1, String s2) {
        try {
            if (s1.equals("update")) {
                St.executeUpdate(s2);
            } else {
                Rs = St.executeQuery(s2);
            }
        } catch (Exception e) {
            if ((e + "").contains("IntegrityConstraintViolation")) {
                JOptionPane.showMessageDialog(null, "Cuenta Utilizada Actualmente");
            } else {
                JOptionPane.showMessageDialog(null, e + "\nen Coneccion a Base de Datos");
            }
        }
    }
}
