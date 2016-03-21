package firedetection;
import java.sql.*;
import javax.swing.*;
import java.util.*;
import javax.swing.table.*;

public class BD extends javax.swing.JPanel {
       
    final double tr_threshold=1.01,tl_threshold=1.1,tm_threshold=2,th_threshold=1;// threshold values
    String host,usuario,contra;//DB connection parameters
    boolean error;
    Connection cone;
    Statement St;
    ResultSet Rs;
    LinkedList<Registro> RegList;
    LinkedList<Registro> Detections;
    double[] avg, sum;
    int windowSize,FirstIndex=0,LastIndex; //variables para threshold
    int curState,count,count2,ocurrencies=3; //variables de maquina de estados
    long time;
    boolean found=false;
    DefaultTableModel model;
 
    public BD() {//creamos coneccion a BD y realizamos una prueba, si no cumple se cambian datos
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        initComponents();
        host="localhost";
        usuario="itm";
        contra="itmexicali";
        jFormattedTextField1.setText("5");
        jTextField1.setText("select * from muestras where date(fechahora)='2011-08-26';");
        try{
            Class.forName("com.mysql.jdbc.Driver");
            cone=DriverManager.getConnection("jdbc:mysql://"+host+"/"+"bdmts400"+"?user="+usuario+"&password="+contra);
            St=cone.createStatement();
            Rs=St.executeQuery("select 1;");
        }catch(Exception e){CambiarDatos();}
        
    }
    
    public void init(){//inicializamos valores
        FirstIndex=0;
        RegList=new LinkedList();
        Detections=new LinkedList();
        avg=new double[3];
        sum=new double[3];
        
    }
    
    public void threshold(){
        curState=count=count2=0;//se inicializan los valores
        time=System.currentTimeMillis();// se toma el tiempo de inicio
        while(LastIndex<RegList.size()){//se recorren todos los registros
/*         if(found){  
              if(RegList.get(LastIndex).temp<avg[0])
                found=false;     
         }
         else{
 */            switch(curState){
                 case 0: //temperatura
                        if(state0()){//temperatura aumenta
                            if(++count==ocurrencies){
                                curState++;
                                count=count2=0;}
                        }else  count=0;
                        break;
                     
                 case 1: //temperatura y luz
                        if(state0()){//temperatura sigue aumentando
                            if(state1()){//iluminacion aumenta
                                count2=0;
                                if(++count==ocurrencies){
                                    curState++;
                                    count=0;
                                    }
                            }else{//iluminacion disminuye
                                count=0;
                                if(++count2==ocurrencies){
                                    curState+=2;
                                    count=0;
                                }
                            }
                        }else{ //si temperatura dejo de aumentar nos regresamos
                            count=0;
                            curState--;
                        }
                        break;
                     
                 case 2: //temperatura, iluminacion y !humedad
                        if(state0()&&state1()&&!state23()){
                            if(++count==ocurrencies){
                                curState=5;
                                count=0;}
                        }else  {count=0; curState=0;}
                        break;
                     
                 case 3://temperatura, !iluminacion y !humedad
                        if(state0()&&!state1()&&!state23()){
                            if(++count==ocurrencies){
                                curState++;
                                count=0;}
                        }else  {curState=0; count=0;}
                        break;
                 
                  case 4://temperatura, !humedad y avgTemp-temp0>=3;
                        if(state4()){
                                curState=5;
                        }else   curState=0;
                        break;
                  
                  case 5:
                        Detections.add(RegList.get(LastIndex));
                        curState=0;
                        break;
                  default:break;
             }
 //       }  
        updateAvg();  
        FirstIndex++;
        LastIndex++;   
        }
        System.out.printf("Termino metodo Threshold en: %d con %d detecciones\n",(System.currentTimeMillis()-time),Detections.size());
        jLabel3.setText("<html><font color=blue>"+Detections.size()+"</font> detections</html>");
        jLabel4.setText("<html>in <font color=blue>"+redondear(((double)System.currentTimeMillis()-time)/1000,2)+" </font>secs</html>");
        updateTable();
    }
    
    
    
    public void threshold1(){
        time=System.currentTimeMillis();//se toma el tiempo de entrada
        while(LastIndex<RegList.size()){//recorrido de todos los registros
            if(found){  
                  if(RegList.get(LastIndex).temp<avg[0])//si se encontro 1 incendio se deja d 
                  found=false;                          //checar asta que vuelva a decrecer la temperatura
            }else{
                if(state0()){//checa avgTemp > threshold
                   if(state1()){//checa iluminacion > prom
                       if(state23()){//checa humedad
                           Detections.add(RegList.get(LastIndex));
                           found=true; 
                       }
                     }
                    else{ //iluminacion <=
                        if(state23()){//checa humedad
                             if(state4()){//TempFin-TempIni > 3grados
                                Detections.add(RegList.get(LastIndex));
                                found=true;
                             }
                        }
                   }
               }
        }  
        updateAvg();  
        FirstIndex++;
        LastIndex++;   
        }
        System.out.printf("Termino metodo Threshold1 en: %d con %d detecciones\n",(System.currentTimeMillis()-time),Detections.size());
        jLabel3.setText("<html><center><font color=blue>"+Detections.size()+"</font> detections</center></html>");
        jLabel4.setText("<html><center>in <font color=blue>"+redondear(((double)System.currentTimeMillis()-time)/1000,2)+" </font>secs</center</html>");
        updateTable();
    }
    
    public boolean state0(){
        if((RegList.get(LastIndex).temp/avg[0])>tr_threshold){
           return true;
        } 
        return false;}
    
    public boolean state1(){
         if((RegList.get(LastIndex).vis/avg[1])>tl_threshold){
                return true;
            }
        return false;}
    
    public boolean state23(){
        //RegList.get(LastIndex).hum/avg[2]
        if((avg[2]/RegList.get(LastIndex).hum)>th_threshold){
                return true;
        } 
        return false;}
    
    public boolean state4(){
         if((RegList.get(LastIndex).temp-RegList.get(FirstIndex).temp)>=tm_threshold){
                return true;
            }
        return false;}
    
    public boolean cargarBD(String query){//cargamos la base de datos a la lista de registros
         try {
             if(coneccionBD("select",query)){
                 while(Rs.next()){
                    RegList.add(new Registro(Rs.getString("fechahora"),Rs.getDouble("temperature"),Rs.getDouble("humidity"),Rs.getInt("vis_light")));              
                 }
                 avg();//se le saca la media a todos los parametros de los registros
             }
             else return false;
        } catch (Exception ex) { ex.printStackTrace(); return false;}
         return true;
    }
    
    public void avg(){ //parametros: 0=temperatura, 1=vis_light, 2=humedad
        //calculamos el promedio de los parametros de los registros
         for(int i=FirstIndex;i<LastIndex;i++){
            sum[0]+=RegList.get(i).temp;
            sum[1]+=RegList.get(i).vis;
            sum[2]+=RegList.get(i).hum;
        }
        for(int i=0;i<3;i++){
            avg[i]=redondear(sum[i]/windowSize,2);}
    }
    
    public void updateAvg(){//actualzamos los promedios sumando el ultimo, qitando el primero.
        sum[0]=sum[0]+RegList.get(LastIndex).temp-RegList.get(FirstIndex).temp;
        sum[1]=sum[1]+RegList.get(LastIndex).vis-RegList.get(FirstIndex).vis;
        sum[2]=sum[2]+RegList.get(LastIndex).hum-RegList.get(FirstIndex).hum;
        for(int i=0;i<3;i++){
             avg[i]=redondear(sum[i]/windowSize,2);}
    }
    
    public void updateTable(){//cargamos los datos a la tabla
         model=(new DefaultTableModel(new Object [][] {},new String [] {"Date & Time", "Temperature", "Humidity", "Visibility"}));
         for(int i=0;i<Detections.size();i++){
             model.addRow(new Object[]{Detections.get(i).fecha,Detections.get(i).temp,Detections.get(i).hum,Detections.get(i).vis});
         }
         jTable1.setModel(model);
    }
    
    public double redondear(double doble,int digitos){
    int cifras=(int) Math.pow(10,digitos);
    return Math.rint(doble*cifras)/cifras;
	}
    
    public void CambiarDatos(){
        host=JOptionPane.showInputDialog("Localizacion del Host",host);
        usuario=JOptionPane.showInputDialog("Usuario MySQL",usuario);
        contra=JOptionPane.showInputDialog("Contraseña MySQL",usuario);
        try{
        Class.forName("com.mysql.jdbc.Driver");
        cone=DriverManager.getConnection("jdbc:mysql://"+host+"/"+"bdmts400"+"?user="+usuario+"&password="+contra);
        St=cone.createStatement();
        }catch(Exception e){JOptionPane.showMessageDialog(null,e+"\nen CambiarDatos");}    
    }

    public boolean coneccionBD(String s1, String s2){
        try{
            if(s1.equals("update"))    St.executeUpdate(s2);
            else                       Rs=St.executeQuery(s2);
        }catch(Exception e){JOptionPane.showMessageDialog(Interfaz.f,"Error en consulta:\n Coneccion a Base de Datos");
                             return false;}
        return true;
    }
        
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jFormattedTextField1 = new javax.swing.JFormattedTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();

        jLabel1.setText("Window Size:");

        jFormattedTextField1.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("###0;(###0)"))));
        jFormattedTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextField1ActionPerformed(evt);
            }
        });

        jLabel2.setText("DB Query:");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton1.setText("Execute");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Date & Time", "Temperature", "Humidity", "Visibility"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setCellSelectionEnabled(true);
        jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));
        jTable1.setDebugGraphicsOptions(javax.swing.DebugGraphics.BUFFERED_OPTION);
        jTable1.setDoubleBuffered(true);
        jTable1.setDragEnabled(true);
        jTable1.setDropMode(javax.swing.DropMode.ON_OR_INSERT_ROWS);
        jTable1.setFillsViewportHeight(true);
        jTable1.setFocusable(false);
        jTable1.setGridColor(new java.awt.Color(14, 195, 240));
        jTable1.setNextFocusableComponent(this);
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jTable1.setSurrendersFocusOnKeystroke(true);
        jScrollPane1.setViewportView(jTable1);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Threshold", "Threshold1" }));
        jComboBox1.setToolTipText("Select method to use");
        jComboBox1.setAutoscrolls(true);
        jComboBox1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        jLabel5.setText("Method:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jComboBox1, 0, 149, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton1))
                            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jFormattedTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextField1ActionPerformed
    
    }//GEN-LAST:event_jFormattedTextField1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
       
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        init();
        //checa que tamano ventana y query no esten vacias
        if(jFormattedTextField1.getText().isEmpty()||jTextField1.getText().isEmpty())
            JOptionPane.showMessageDialog(Interfaz.f,"Datos Incorrectos!","Error", JOptionPane.OK_OPTION);
        else {
            LastIndex=windowSize=Integer.parseInt(jFormattedTextField1.getText());
            if(cargarBD(jTextField1.getText()))//checar que la query sea adecuada
            switch(jComboBox1.getSelectedIndex()){//se selecciona el metodo que se llamara
                case 0: threshold();break;
                case 1: threshold1();break;
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton jButton1;
    public javax.swing.JComboBox jComboBox1;
    public javax.swing.JFormattedTextField jFormattedTextField1;
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JLabel jLabel3;
    public javax.swing.JLabel jLabel4;
    public javax.swing.JLabel jLabel5;
    public javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTable jTable1;
    public javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
