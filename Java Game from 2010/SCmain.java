
package SC;

import javax.swing.JApplet;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.*;
import javax.swing.JOptionPane;
import java.applet.AudioClip;
import java.awt.MultipleGradientPaint;
import java.awt.image.*;

/**
 *
 * @author cheeese*-*
 */
public class SCmain extends JApplet {
   
    public void init() {
        setContentPane(new SCPanel());        
    }

    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////

    
    public class ObjectHandler{
        int index;
        Gun gun; 
        OISI oisi; 
        static final int NONE=0;
        static final int TEXAN=1;
        static final int BOBBER=2;
        static final int POPPER=3;
        static final int PLATE=4;
        Color GRAY=Color.getHSBColor(0.0f, 0.0f, 0.3f);
        Color LGRAY=Color.getHSBColor(0.0f, 0.0f, 0.8f);
        Color RED=Color.getHSBColor(0.0f, 0.63f, 0.69f);
        boolean override;

        
        public float[][] RotateZ(double angle,int numVector,float[][] vector){
            float buffer0,buffer1;

                
                double[][] rTensor={{Math.cos(angle),-Math.sin(angle)},
                                    {Math.sin(angle),Math.cos(angle)}};

                for (int i=0;i<numVector;i++){                   
                buffer0=vector[i][0];
                buffer1=vector[i][1];                    
                buffer0=(float)(rTensor[0][0]*vector[i][0])+(float)(rTensor[0][1]*vector[i][1]);
                buffer1=(float)((rTensor[1][0]*vector[i][0])+(rTensor[1][1]*vector[i][1]));                    
                vector[i][0]=buffer0;
                vector[i][1]=buffer1;
                }               
                return vector;
        }

 ///////////////////////////////////////////////////////////////////////////////////
        
         public class OISI{
            private ObjectHandler[] data; 
            
        public OISI(){
              data = new ObjectHandler[1];
        }

            
           public ObjectHandler extract(int position) {
              if (position >= data.length)
                 return null;
              else
                 return data[position];
           }


           
           public void add(ObjectHandler object) {
                 int newSize =data.length+1;
                 ObjectHandler[] newData = new ObjectHandler[newSize];
                 System.arraycopy(data, 0, newData, 0, data.length);
                 data = newData;
                 object.index=newSize-1;
                 data[newSize-1]=object;

              }

          
           public void remove(int indx){
               int newSize =data.length-1;
                 ObjectHandler[] newData = new ObjectHandler[newSize];
                 for(int i=1;i<data.length;i++){
                     if(i>=indx){                         
                         if(data.length-1>i){
                         data[i+1].index=i;
                         newData[i]=data[i+1];
                         }
                     }
                     else{
                         newData[i]=data[i];
                     }
                 }
                 data = newData;
           }

           public void discardAll(){
               data = new ObjectHandler[1];
           }

           }

    ////////////////////////////////////////////////////////////////////////////

        public class EjectShell extends ObjectHandler{
            int iterationCase;
            int[][] vectorShell;
            boolean once,empty;
            ResourceHandler resourceH;


            public void RecalculatePosition(){
                 vectorShell[1][0]=100;
                 vectorShell[1][1]=-100-Math.round((float)(250*Math.sin(Math.PI+(0.02*iterationCase*Math.PI))));
                 vectorShell[0][0]+=vectorShell[1][0];
                 vectorShell[0][1]+=vectorShell[1][1];
                 iterationCase+=1;

            }

            public void AnimateObject(Graphics2D g,ImageObserver obs){
                if (iterationCase<30){
                 g.setColor(Color.BLACK);
                 g.translate(vectorShell[0][0], vectorShell[0][1]);
                 g.rotate(iterationCase*Math.PI/8);
                 if(empty){
                    g.drawImage(resourceH.shell, null,0,0);
                 }else{
                     g.drawImage(resourceH.round, null,0,0);
                 }
                 g.rotate(-iterationCase*Math.PI/8);
                 g.translate(-vectorShell[0][0], -vectorShell[0][1]);
                }
                else{
                    if(once){
                    oisi.remove(this.index);
                    once=false;
                    }
                }

            }

            public EjectShell(int x,int y,ResourceHandler rhl,boolean emt){
                once=true;
                empty=emt;
                iterationCase=1;
                vectorShell=new int[2][2];
                vectorShell[0][0]=x;
                vectorShell[0][1]=y;
                resourceH=rhl;
            }
        }

  /////////////////////////////////////////////////////////////////////////////

 ///////////////////////////////////////////////////////////////////////////////////

        
        public class Bobber extends ObjectHandler{
            
            int centerX,centerY,diameter,diameterS,distance,oPosX,oPosY;
            Ellipse2D.Float[] plates;
            float[][] plateVectors;
            float omega;
            float[] mass;
            boolean shoot,stop,locked,periodic;
            float velocity;

            
            public Bobber(int cX,int cY,int dia,int diaS,int dist,double angle,
                    float vel,boolean per,float wei,boolean sht,boolean stp,boolean lk){
                float buffer[]={wei,0.0000001f};
                plates=new Ellipse2D.Float[2];
                //vektorok inicializalasa
                float[][] bufferV={{0,-dia},{0,dia}};
                plateVectors=bufferV;
                mass=buffer;
                omega=0.0f;
                velocity=vel;
                shoot=sht;
                centerX=cX;
                oPosX=cX;
                centerY=cY;
                oPosY=cY;
                diameter=dia;
                diameterS=diaS;
                distance=dist;
                periodic=per;
                stop=stp;
                locked=lk;
                
                plateVectors=RotateZ(angle,2,plateVectors);
                for(int i=0;i<2;i++){
                    plates[i]=new Ellipse2D.Float(centerX+plateVectors[i][0]-(diameterS/2.0f),
                                                  centerY+plateVectors[i][1]-(diameterS/2.0f),
                                                  diameterS, diameterS);
                }
               
                

            }



            
            public void RecalculatePosition(){
                float epsylon;
                epsylon=0.0f;
                if(!locked){
                    if(Math.abs(centerX-oPosX)>=distance){
                        velocity*=-1;
                    }
                centerX+=velocity;
                
                for(int i=0;i<2;i++){
                    epsylon=epsylon+(plateVectors[i][0]*mass[i]*9.81f);
                }
               
                omega+=epsylon;                
                plateVectors=RotateZ(omega,2,plateVectors);
                for(int i=0;i<2;i++){
                    plates[i].x=centerX+plateVectors[i][0]-(diameterS/2.0f);
                    plates[i].y=centerY+plateVectors[i][1]-(diameterS/2.0f);
                }
                }
            }

            public void AnimateObject(Graphics2D g,ImageObserver obs){               
                 g.setColor(GRAY);
                g.drawLine(centerX, centerY, Math.round(centerX-(diameter/4f)), Math.round(centerY+diameter*1.3f));
                g.drawLine(centerX, centerY, Math.round(centerX+(diameter/4f)), Math.round(centerY+diameter*1.3f));
                    for(int i=0;i<2;i++){
                    g.setColor(GRAY);                    
                    g.drawLine(centerX, centerY, Math.round(plates[i].x+(plates[i].width/2)),
                            Math.round(plates[i].y+(plates[i].height/2)));
                    }
                 if(mass[1]!=0){
                     if(shoot){
                        g.setColor(LGRAY);
                        }else{
                            if(stop){
                            g.setColor(Color.ORANGE);
                    }else{
                        g.setColor(RED);
                    }
                }
                 g.fillOval(Math.round(plates[1].x),
                             Math.round(plates[1].y),
                             Math.round(plates[1].width),
                             Math.round(plates[1].height));
                 }
                    g.setColor(GRAY);
                    g.fillOval(Math.round(plates[0].x),
                             Math.round(plates[0].y),
                             Math.round(plates[0].width),
                             Math.round(plates[0].height));
             

            }

            
            public boolean TestContain(int x,int y,Timing time){
                override=false;
                 if(plates[1].contains(x, y)){
                     if(shoot&&!stop){
                     mass[1]=0.0f;
                     }else{
                         if(!stop){
                            time.HitNoShoot();
                            override=true;
                         }else{
                             time.HitStop();
                             mass[1]=0.0f;
                         }
                }
                 }

               return false;
            }

            public int CountAlive(){
                if(!shoot||stop){
                    return 0;
                }
                if(mass[1]==0){
                    return 0;
                }else{
                    return 1;
                }
            }
        }

 //////////////////////////////////////////////////////////////////////////////////

        public class Background extends ObjectHandler{
            ResourceHandler resourceH;
            public Background(ResourceHandler rhl){
                resourceH=rhl;
            }




            public void RecalculatePosition(){}

            public void AnimateObject(Graphics2D g,ImageObserver obs){
                g.drawImage(resourceH.bg, 0, 0, obs);
            }

           
            public boolean TestContain(int x,int y,Timing time){

               return false;
            }
        }

 /////////////////////////////////////////////////////////////////////////////////

        public class House extends ObjectHandler{
            ResourceHandler resourceH;
            int posX,posY;
            public House(ResourceHandler rhl,int x,int y){
                resourceH=rhl;
                posX=x;
                posY=y;
            }




            public void RecalculatePosition(){}

            public void AnimateObject(Graphics2D g,ImageObserver obs){
                g.drawImage(resourceH.house1, posX, posY, obs);
            }


            public boolean TestContain(int x,int y,Timing time){
               return false;
            }
        }

 /////////////////////////////////////////////////////////////////////////////////


        public class HouseInner extends ObjectHandler{
            ResourceHandler resourceH;
            int posX,posY;
            public HouseInner(ResourceHandler rhl,int x,int y){
                resourceH=rhl;
                posX=x;
                posY=y;
            }




            public void RecalculatePosition(){}

            public void AnimateObject(Graphics2D g,ImageObserver obs){
                g.drawImage(resourceH.house2, posX, posY, obs);
            }


            public boolean TestContain(int x,int y,Timing time){

               return false;
            }
        }

 /////////////////////////////////////////////////////////////////////////////////
        
        public class Barrel extends ObjectHandler{
            ResourceHandler resourceH;
            int posX,posY;
            boolean stanceSt;
            double scale;

            public Barrel(ResourceHandler rhl,int x,int y,boolean stc,double scl){
                resourceH=rhl;
                posX=x;
                posY=y;
                stanceSt=stc;
                scale=scl;
            }




            public void RecalculatePosition(){}

            public void AnimateObject(Graphics2D g,ImageObserver obs){
                if(stanceSt){
                    g.translate(posX, posY);
                    g.scale(scale, scale);
                    g.drawImage(resourceH.barrel, 0, 0, obs);
                    g.scale(1/scale, 1/scale);
                    g.translate(-posX, -posY);
                }else{
                    g.translate(posX, posY);
                    g.rotate(Math.PI/2);
                    g.scale(scale, scale);
                    g.drawImage(resourceH.barrel, 0, 0, obs);
                    g.scale(1/scale, 1/scale);
                    g.rotate(-Math.PI/2);
                    g.translate(-posX, -posY);
                }
            }


            public boolean TestContain(int x,int y,Timing time){
               return false;
            }
        }

 /////////////////////////////////////////////////////////////////////////////////

        public class Plate extends ObjectHandler{

            int centerX,centerY,diameterS,distance,oPosX,oPosY;
            Ellipse2D.Float plates;
            float mass;
            boolean shoot,stop,locked,periodic;
            float velocity;
            int connection1,connection2,connectionType1,connectionType2;
            


            public Plate(int cX,int cY,int diaS,int dist,
                    float vel,boolean per,float wei,boolean sht,boolean stp,boolean lk){

                mass=wei;
                velocity=vel;
                shoot=sht;
                centerX=cX;
                oPosX=cX;
                centerY=cY;
                oPosY=cY;
                diameterS=diaS;
                distance=dist;
                periodic=per;
                stop=stp;
                locked=lk;
                connection1=0;
                connection2=0;
                connectionType1=NONE;
                connectionType2=NONE;
                

                    plates=new Ellipse2D.Float(centerX-(diameterS/2.0f),
                                                  centerY-(diameterS/2.0f),
                                                  diameterS, diameterS);
            }

            public void AddConnection(int connectionType,int connection){
                if(connection1==0&&connectionType1==0){
                    connection1=connection;
                    connectionType1=connectionType;
                }else{
                    if(connection2==0&&connectionType2==0){
                        connection2=connection;
                    connectionType2=connectionType;
                    }
                }

            }
            
            public void DRAGON(int RUN){
                //        .==.        .==.
                //       //`^\\      //^`\\
                //      // ^ ^\(\__/)/^ ^^\\
                //     //^ ^^ ^/6  6\ ^^ ^ \\
                //    //^ ^^ ^/( .. )\^ ^ ^ \\
                //   // ^^ ^/\| v""v |/\^ ^ ^\\
                //  // ^^/\/ /  `~~`  \ \/\^ ^\\
                //  -----------------------------
                /// LET THERE BE DRAGONS
            }

            public void TriggerConnection(OISI oisi){
                if(connection1!=0&&connectionType1!=0){
                    switch(connectionType1){
                        case TEXAN:{
                            TexasStar temp=(TexasStar)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                        case BOBBER:{
                            Bobber temp=(Bobber)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                        case POPPER:{
                            Popper temp=(Popper)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                        case PLATE:{
                            Plate temp=(Plate)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                    }
                }

                if(connection2!=0&&connectionType2!=0){
                    switch(connectionType2){
                        case TEXAN:{
                            TexasStar temp=(TexasStar)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                        case BOBBER:{
                            Bobber temp=(Bobber)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                        case POPPER:{
                            Popper temp=(Popper)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                        case PLATE:{
                            Plate temp=(Plate)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                    }
                }
            }


            public void RecalculatePosition(){
                if(!locked){
                    if(Math.abs(centerX-oPosX)>=distance){
                        velocity*=-1;
                        if(!periodic){
                            locked=true;
                        }
                    }
                centerX+=velocity;
                plates.x=centerX-(diameterS/2.0f);
                }
            }

            public void AnimateObject(Graphics2D g,ImageObserver obs){
               
                 if(mass!=0){
                     if(shoot){
                        g.setColor(LGRAY);
                        }else{
                            if(stop){
                            g.setColor(Color.ORANGE);
                    }else{
                        g.setColor(RED);
                    }
                }
                 g.fillOval(Math.round(plates.x),
                             Math.round(plates.y),
                             Math.round(plates.width),
                             Math.round(plates.height));
                 }


            }

            public boolean TestContain(int x,int y,Timing time){
                override=false;
                 if(plates.contains(x, y)&&mass!=0){
                     if(shoot&&!stop){
                     mass=0.0f;
                     return true;
                     }else{
                         if(!stop){
                            time.HitNoShoot();
                            override=true;
                            return false;
                         }else{
                             if(!shoot){
                             time.HitStop();
                             mass=0.0f;
                             return false;
                             }else{
                                 override=true;
                                 return false;
                             }
                         }
                }
                 }else {return false;}
            }

            public int CountAlive(){
                if(!shoot||stop){
                    return 0;
                }
                if(mass==0){
                    return 0;
                }else{
                    return 1;
                }
            }

        }
 ////////////////////////////////////////////////////////////////////////////////


        public class Popper extends ObjectHandler{

            int centerX,centerY,height,distance,oPosX,oPosY;
            Ellipse2D.Float plates;
            Rectangle2D.Float square;
            float mass;
            boolean shoot,locked,periodic;
            float velocity;
            int connection1,connection2,connectionType1,connectionType2;


            public Popper(int cX,int cY,int hei,int dist,
                    float vel,boolean per,float wei,boolean sht,boolean lk){

                mass=wei;
                velocity=vel;
                shoot=sht;
                centerX=cX;
                oPosX=cX;
                centerY=cY;
                oPosY=cY;
                height=hei;
                distance=dist;
                periodic=per;
                locked=lk;
                connection1=0;
                connection2=0;
                connectionType1=NONE;
                connectionType2=NONE;
                

                square=new Rectangle2D.Float(centerX-(height/4f),centerY-(height/2f),height/2f,height);
                plates=new Ellipse2D.Float(centerX-(height/4f),centerY-(height/2f)-(height/2.2f),
                                                  height/2f, height/2f);

            }

            public void AddConnection(int connectionType,int connection){
                if(connection1==0&&connectionType1==0){
                    connection1=connection;
                    connectionType1=connectionType;
                }else{
                    if(connection2==0&&connectionType2==0){
                        connection2=connection;
                    connectionType2=connectionType;
                    }
                }

            }

            public void TriggerConnection(OISI oisi){
                if(connection1!=0&&connectionType1!=0){
                    switch(connectionType1){
                        case TEXAN:{
                            TexasStar temp=(TexasStar)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                        case BOBBER:{
                            Bobber temp=(Bobber)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                        case POPPER:{
                            Popper temp=(Popper)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                        case PLATE:{
                            Plate temp=(Plate)oisi.data[connection1];
                            temp.locked=false;
                            break;
                        }
                    }
                }

                if(connection2!=0&&connectionType2!=0){
                    switch(connectionType2){
                        case TEXAN:{
                            TexasStar temp=(TexasStar)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                        case BOBBER:{
                            Bobber temp=(Bobber)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                        case POPPER:{
                            Popper temp=(Popper)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                        case PLATE:{
                            Plate temp=(Plate)oisi.data[connection2];
                            temp.locked=false;
                            break;
                        }
                    }
                }
            }


            public void RecalculatePosition(){
                if(!locked){
                    if(Math.abs(centerX-oPosX)>=distance){
                        velocity*=-1;
                        
                    }
                    centerX+=velocity;
                    square.x=centerX-(height/8f);
                    plates.x=centerX-(height/8f);
                }
            }

            public void AnimateObject(Graphics2D g,ImageObserver obs){

                 if(mass!=0){
                     if(shoot){
                        g.setColor(LGRAY);
                        }else{
                        g.setColor(RED);

                        }

                 g.fillOval(Math.round(plates.x),
                             Math.round(plates.y),
                             Math.round(plates.width),
                             Math.round(plates.height));
                 g.fillRect(Math.round(square.x),
                             Math.round(square.y),
                             Math.round(square.width),
                             Math.round(square.height));
                 }
            }


            public boolean TestContain(int x,int y,Timing time){
                override=false;
                 if((plates.contains(x, y)||square.contains(x, y))&&mass!=0){
                     if(shoot){
                     mass=0.0f;
                     return true;
                     }else{
                            time.HitNoShoot();
                            override=true;
                            return false;
                     }
                 }else {return false;}
            }

            public int CountAlive(){
                if(!shoot){
                    return 0;
                }
                if(mass==0){
                    return 0;
                }else{
                    return 1;
                }
            }

        }

            
 /////////////////////////////////////////////////////////////////////////////////

        
        public class TexasStar extends ObjectHandler{
            
            int centerX,centerY,diameter,diameterS,distance,oPosX,oPosY;
            Ellipse2D.Float[] plates;
            float[][] plateVectors;
            float omega,velocity;
            float[] mass;
            boolean shoot,locked;

            
            public TexasStar(int cX,int cY,int dia,int diaS,int dist,float omg,
                    float vel,float wei,boolean sht,boolean lkt){
                float buffer[]={wei,wei,wei,wei,wei};//0.00001f
                plates=new Ellipse2D.Float[5];
                plateVectors=new float[5][2];
                mass=buffer;
                omega=omg;
                velocity=vel;
                shoot=sht;
                centerX=cX;
                oPosX=cX;
                centerY=cY;
                oPosY=cY;
                diameter=dia;
                diameterS=diaS;
                distance=dist;
                locked=lkt;
                
                for(int i=0;i<5;i++){
                plateVectors[i][0]=(float)((diameter/2)*Math.cos(i*(Math.PI*2)/5));
                plateVectors[i][1]=(float)((diameter/2)*Math.sin(i*(Math.PI*2)/5));
                }
               
                for(int i=0;i<5;i++){
                    plates[i]=new Ellipse2D.Float(centerX+plateVectors[i][0]-(diameterS/2.0f),
                                                  centerY+plateVectors[i][1]-(diameterS/2.0f),
                                                  diameterS, diameterS);
                }

            }

           
            public void RecalculatePosition(){
                float epsylon;
                epsylon=0.0f;
                if(!locked){
                if(Math.abs(oPosX-centerX)>=distance){
                        velocity*=-1;
                    }
                centerX+=velocity;

                
                for(int i=0;i<5;i++){
                    epsylon=epsylon+(plateVectors[i][0]*mass[i]*9.81f);
                }
                
                omega+=epsylon;               
                plateVectors=RotateZ(omega,5,plateVectors);                
                for(int i=0;i<5;i++){
                    plates[i].x=centerX+plateVectors[i][0]-(diameterS/2.0f);
                    plates[i].y=centerY+plateVectors[i][1]-(diameterS/2.0f);
                }
                }

                

            }

            public void AnimateObject(Graphics2D g,ImageObserver obs){               
                g.setColor(GRAY);
                g.drawLine(centerX, centerY, Math.round(centerX-(diameter/4f)), Math.round(centerY+diameter*0.8f));
                g.drawLine(centerX, centerY, Math.round(centerX+(diameter/4f)), Math.round(centerY+diameter*0.8f));
                for (int i=0;i<5;i++){
                    g.setColor(GRAY);
                    g.drawLine(centerX, centerY, Math.round(plates[i].x+(plates[i].width/2)),
                            Math.round(plates[i].y+(plates[i].height/2)));
                 if(mass[i]!=0){
                     if(shoot){
                        g.setColor(LGRAY);
                        }else{
                            g.setColor(RED);
                        }
                 g.fillOval(Math.round(plates[i].x),
                             Math.round(plates[i].y),
                             Math.round(plates[i].width),
                             Math.round(plates[i].height));
                 }
                 
                }

            }

            
            public boolean TestContain(int x,int y,Timing time){
                override=false;
                for(int i=0;i<5;i++){
                 if(plates[i].contains(x, y)){
                     if(shoot){
                     mass[i]=0.0f;
                     locked=false;
                     }else{
                        time.HitNoShoot();
                        override=true;
                     }
                 }
               }
                return false;
            }

            public int CountAlive(){
                int num=0;
                if(!shoot){
                    return 0;
                }
                for(int i=0;i<5;i++){
                    if(mass[i]==0){
                        num+=0;
                    }else{
                       num+=1;
                    }
                }
                return num;
            }

        }

 ////////////////////////////////////////////////////////////////////////////////////////

        
        public class Gun extends ObjectHandler{
            int currentState,prevState; //guess it
            int lengthofRecoil=2;
            final short lengthofReload=10;
            int lengthCurrentReload,lengthCurrentRecoil;
            int numRds;
            int prevX,prevY,posX,posY,HandX,HandY,finalHandX,finalHandY,posHandX,posHandY;
            int distancePost,distance,errX,errY;
            int[] vectorMouse,numMags;
            ResourceHandler resourceH;
            final short NOMAG=33;
            boolean nomag,isReloading;

            
            public Gun(ResourceHandler rhl){
                currentState=2;
                nomag=true;
                lengthCurrentReload=0;
                lengthCurrentRecoil=0;
                numRds=-1;
                int[] buffer={-1,15,15,15,15,-1,-1,-1,-1,-1};
                numMags=buffer;
                distancePost=100;
                distance=500;
                vectorMouse=new int[2];
                resourceH=rhl;
                prevX=450;
                prevY=920;
                posY=920;
                posX=450;
            }

            
            public void SetState(int stateNo){
                switch(stateNo){
                    case 0:{
                        if(currentState!=2){
                            lengthofRecoil=10;
                        }
                        currentState=1;
                        break;
                    }
                    case 1:{
                        if(prevState==1){
                            currentState=1;
                            lengthofRecoil=2;
                            posY-=50;
                        }
                        break;
                    }
                    case 3:{
                        if(nomag){
                            if(currentState==2){
                                currentState=3;
                                isReloading=true;
                            }
                            else{
                                currentState=4;
                                isReloading=true;
                            }
                        }
                        break;
                    }
                    case NOMAG:{
                       nomag=true;                       
                       numRds=0;
                       break;  
                    }
                }
            }

            public void SelectSlot(int slotNum,ResourceHandler rh){                
                    if(isReloading){
                        if(numMags[slotNum]!=-1){
                        numRds=numMags[slotNum];
                        numMags[slotNum]=-1;
                        isReloading=false;
                        rh.magin.play();
                        nomag=false;
                        }
                }
                
            }

            
            public void RecalculateGunMouse(int x,int y,boolean stb,boolean rdy){
                
                vectorMouse[0]=x-posX;
                vectorMouse[1]=y-posY;
                prevX=x;
                if(y>50){
                prevY=y;
                }
                else{
                    prevY=50;
                }
                if(rdy||stb){
                    if(y>620){
                        prevY=y;
                    }
                    else{
                        prevY=620;
                }
                }
                
            }

           
            public void RecalculatePosition(){

                vectorMouse[0]=prevX-posX;
                 vectorMouse[1]=prevY-posY;
                 if(vectorMouse[0]>1||vectorMouse[1]>1||
                         vectorMouse[0]<-1||vectorMouse[1]<-1){
                 posX=posX+Math.round(0.3f*vectorMouse[0]);
                 posY=posY+Math.round(0.3f*vectorMouse[1]);
                 }else{
                     posX=prevX;
                     posY=prevY;
                 }
                
                 errX=prevX+Math.round((distance/distancePost)*(prevX-posX));
                 errY=prevY+Math.round((distance/distancePost)*(prevY-posY));

                 
                if(lengthCurrentReload>lengthofReload){
                    if(currentState==4){
                        currentState=0;
                    }
                    else{if(currentState==3){
                        currentState=2;
                    }
                    }
                    lengthCurrentReload=0; 
                }
                 if(lengthCurrentReload<=lengthofReload&&!isReloading&&(currentState==4||currentState==3)){
                     HandX=posX-140-397;
                     HandY=posY+332-0;
                     finalHandX=posX-140-206;
                     finalHandY=posY+332-276;
                     posHandX=HandX+Math.round(lengthCurrentReload*((finalHandX-HandX)/10f));
                     posHandY=HandY+Math.round(lengthCurrentReload*((finalHandY-HandY)/10f));
                     lengthCurrentReload+=1;
                 }else{
                     lengthCurrentReload=0;
                     posHandX=posX-140-206;
                     posHandY=posY+332-276;
                 }

                 
                if(lengthCurrentRecoil>=lengthofRecoil){
                    if(numRds>=1&&!nomag){
                        prevState=1;
                        currentState=0;
                        numRds-=1;
                    }else{
                        if(nomag){
                            currentState=0;
                            prevState=0;
                        }else{
                            if(prevState==1){
                            currentState=2;
                            prevState=0;
                            }else {
                                currentState=0;
                                prevState=0;
                            }
                        }
                    }
                    lengthCurrentRecoil=0;
                }
                

            }

            
            public void AnimateObject(Graphics2D g,ImageObserver obs){
                g.setColor(Color.BLACK);
                g.fillRect(prevX-3, prevY-3, 6, 10);
                if(nomag){
                 g.setFont(g.getFont().deriveFont(24f));
                 g.drawString("NO MAG", 400, 50);
                }                
                if(isReloading){
                 g.setFont(g.getFont().deriveFont(24f));
                 g.drawString("RELOADING", 200, 50);
                }
                for (int i=0;i<10;i++){
                 g.setFont(g.getFont().deriveFont(18f));
                 g.drawString(String.valueOf(i), i*50+50, 550);
                 if(numMags[i]!=-1){
                 g.drawString(String.valueOf(numMags[i]), i*50+50, 570);
                 }else{
                     g.drawString("empty", i*50+50, 570);
                 }
                }
                
                g.setColor(Color.WHITE);
                g.fillRect(prevX-1, prevY-1, 2, 2);

                switch(currentState){
                    case 0:{
                        g.drawImage(resourceH.usp0,posX-403, posY-4,obs);
                        break;
                    }
                    case 1:{
                        g.drawImage(resourceH.usp2,posX-403, posY-17,obs);
                        lengthCurrentRecoil+=1;
                        break;
                    }
                    case 2:{
                        g.drawImage(resourceH.usp2,posX-403, posY-17,obs);
                        break;
                    }
                    case 3:{
                        g.drawImage(resourceH.relhand, posHandX, posHandY, obs);
                        g.drawImage(resourceH.rel2, posX-136, posY-20,obs);
                        //lengthCurrentReload+=1;
                        break;
                    }
                    case 4:{
                        g.drawImage(resourceH.relhand, posHandX, posHandY, obs);
                        g.drawImage(resourceH.rel0, posX-136, posY-20,obs);
                        //lengthCurrentReload+=1;
                        break;
                    }
                }

            }
        }

 /////////////////////////////////////////////////////////////////////////////////

       
        public void CalculateAll(){
            if (oisi.data.length!=1){
            for (int i=1;i<oisi.data.length;i++){
                oisi.data[i].RecalculatePosition();
            }
            }
        }

        public void AnimateAll(Graphics2D g,ImageObserver obs){
            if (oisi.data.length!=1){
            for (int i=1;i<oisi.data.length;i++){
                oisi.data[i].AnimateObject(g,obs);
            }
            }
        }
        
        public void TestHit(int x,int y,Timing time){
            if (oisi.data.length!=1){
            for (int i=oisi.data.length-1;i>0;i--){
                if(oisi.data[i].TestContain(x,y,time)){
                    oisi.data[i].TriggerConnection(oisi);
                }
                if(oisi.data[i].override){
                    break;
                }
            }
            }
        }

        public int CountAllAlive(){
            int num=0;
            if (oisi.data.length!=1){
                for (int i=oisi.data.length-1;i>0;i--){
                    num+=oisi.data[i].CountAlive();
                }
            }
            return num;
        }

        
        public void CreateGun(ResourceHandler rhl){
            gun=new Gun(rhl);
            oisi.add(gun);
        }

        
        public TexasStar CreateNewTexan(int cX,int cY,int dia,int diaS,int dist,float omg,
                    float vel,float wei,boolean sht,boolean lkt){
            TexasStar temp=new TexasStar(cX,cY,dia,diaS,dist,omg,vel,wei,sht,lkt);
            oisi.add(temp);
            return temp;
        }

        
        public Bobber CreateNewBobber(int cX,int cY,int dia,int diaS,int dist,double angle,
                    float vel,boolean per,float wei,boolean sht,boolean stp,boolean lk){
            Bobber temp=new Bobber(cX,cY,dia,diaS,dist,angle,vel,per,wei,sht,stp,lk);
            oisi.add(temp);
            return temp;
        }

        public Popper CreateNewPopper(int cX,int cY,int hei,int dist,
                    float vel,boolean per,float wei,boolean sht,boolean lk){
            Popper temp=new Popper(cX,cY,hei,dist,vel,per,wei,sht,lk);
            oisi.add(temp);
            return temp;
        }

        public Plate CreateNewPlate(int cX,int cY,int diaS,int dist,
                    float vel,boolean per,float wei,boolean sht,boolean stp,boolean lk){
            Plate temp=new Plate(cX,cY,diaS,dist,vel,per,wei,sht,stp,lk);
            oisi.add(temp);
            return temp;
        }

        public void CreateNewBackground(ResourceHandler rhl){
            oisi.add(new Background(rhl));
        }

        public void CreateNewHouseInner(ResourceHandler rhl,int x,int y){
            oisi.add(new HouseInner(rhl,x,y));
        }

        public void CreateNewHouse(ResourceHandler rhl,int x,int y){
            oisi.add(new House(rhl,x,y));
        }

        public void CreateNewBarrel(ResourceHandler rhl,int x,int y,boolean stc,double scl){
            oisi.add(new Barrel(rhl,x,y,stc,scl));
        }

        public void CreateEjectShell(int x,int y,ResourceHandler rhl,boolean emt){
            oisi.add(new EjectShell(x,y,rhl,emt));
        }
        
        public void RecalculatePosition(){}
        
        
        public void AnimateObject(Graphics2D g,ImageObserver obs){}
        public boolean TestContain(int x,int y,Timing time){
            return false;
        }
        public void TriggerConnection(OISI oisi){}

        public int CountAlive(){return 0;}

////////////////////////////////////////////////////////////////////////////////

        
        public ObjectHandler(){
            oisi=new OISI();
        }
        
    }

////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

    public class Levels{
        static final int DEATH_STAR=0;
        static final int RELOAD=1;
        static final int LEAD_SPRAYER=2;
        static final int THE_CROSS=3;
        static final int HOSTAGE=4;
        static final int DONT_STOP=5;
        static final int TAKE_COVER=6;
        static final int HOSTAGE2=7;
        public Levels(int levelNo,ObjectHandler oh,ResourceHandler rh){
            switch(levelNo){
                case DEATH_STAR:CreateLevelDeathStar(oh,rh);
                                break;
                case RELOAD:CreateLevelReload(oh,rh);
                                break;
                case LEAD_SPRAYER:CreateLevelLeadSprayer(oh,rh);
                                break;
                case THE_CROSS:CreateLevelTheCross(oh,rh);
                                break;
                case HOSTAGE:CreateLevelHostage(oh,rh);
                                break;
                case DONT_STOP:CreateLevelDontStop(oh,rh);
                                break;
                case TAKE_COVER:CreateLevelTakeCover(oh,rh);
                                break;
            }
        }

        public void CreateLevelDeathStar(ObjectHandler oh,ResourceHandler rh){
            oh.CreateNewBackground(rh);
            ObjectHandler.TexasStar t1=oh.CreateNewTexan(200,200,100,20,50,0.1f,1f,0.00001f,true,true);
            ObjectHandler.TexasStar t2=oh.CreateNewTexan(200,200,100,30,50,-0.1f,1f,0.00001f,false,true);
            ObjectHandler.Bobber b1=oh.CreateNewBobber(400, 200, 50, 10, 0, Math.PI/1.8, 0f, true, 0.000015f, false, true, true);
            oh.CreateNewTexan(600,200,100,20,50,0.1f,1f,0.00001f,true,false);
            oh.CreateNewPlate(650, 200, 30, 50, 1f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(640, 170, 30, 50, 1f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(640, 230, 30, 50, 1f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(610, 150, 30, 50, 1f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(350, 190, 30, 50, 0f, true, 0.00001f, false, false, false);
            ObjectHandler.Popper p1=oh.CreateNewPopper(300, 400, 100, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(450, 300, 50, 0, 0, true, 0.00001f, true, false).AddConnection(ObjectHandler.BOBBER, b1.index);
            p1.AddConnection(ObjectHandler.TEXAN, t1.index);
            p1.AddConnection(ObjectHandler.TEXAN, t2.index);
            
            
        }

        public void CreateLevelReload(ObjectHandler oh,ResourceHandler rh){
            oh.CreateNewBackground(rh);
            oh.CreateNewPlate(650, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(600, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(550, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(500, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(400, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(350, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(300, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(250, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(200, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(425, 250, 20, 50, 1f, true, 0.00001f, false, true, true);
        }

        public void CreateLevelLeadSprayer(ObjectHandler oh,ResourceHandler rh){
            oh.CreateNewBackground(rh);
            oh.CreateNewPlate(760, 300, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(730, 300, 20, 50, 1f, true, 0.00001f, false, true, true);
            int ns3=oh.CreateNewPlate(730, 300, 30, 30, 3f, false, 0.00001f, false, false, true).index;
            oh.CreateNewBarrel(rh,700,300,true,0.7);
            int p1=oh.CreateNewPopper(400, 290, 50, 50, 1f, true, 0.00001f, true, true).index;
            oh.CreateNewHouseInner(rh,100,150);
            oh.CreateNewPopper(180, 140, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(300, 140, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPlate(100, 180, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(250, 150, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            int pl1=oh.CreateNewPlate(260, 300, 30, 30, 3f, false, 0.00001f, true, false, true).index;
            oh.CreateNewPlate(260, 300, 40, 50, 1f, true, 0.00001f, true, true, true);
            oh.CreateNewPlate(400, 260, 50, 50, 1f, true, 0.00001f, true, true, true);
            oh.CreateNewPlate(400, 300, 50, 50, 1f, true, 0.00001f, true, true, true);
            oh.CreateNewHouse(rh,100,150);
            oh.CreateNewPlate(530, 300, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(560, 300, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(500, 300, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(590, 300, 20, 50, 1f, true, 0.00001f, true, false, true);
            int ns1=oh.CreateNewPlate(500, 300, 30, 30, 3f, false, 0.00001f, false, false, true).index;
            int ns2=oh.CreateNewPlate(590, 300, 30, 30, -3f, false, 0.00001f, false, false, true).index;
            ObjectHandler.Popper p2=oh.CreateNewPopper(300, 400, 80, 0, 0, true, 0.00001f, true, false);
            ObjectHandler.Popper p3=oh.CreateNewPopper(200, 400, 80, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(650, 250, 40, 0, 0, true, 0.00001f, true, false).AddConnection(ObjectHandler.PLATE, ns3);
            p3.AddConnection(ObjectHandler.POPPER, p1);
            p3.AddConnection(ObjectHandler.PLATE, pl1);
            p2.AddConnection(ObjectHandler.PLATE, ns2);
            p2.AddConnection(ObjectHandler.PLATE, ns1);
            oh.CreateNewPlate(480, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(530, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(580, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(630, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            
            
            


        }

        public void CreateLevelTheCross(ObjectHandler oh,ResourceHandler rh){
            oh.CreateNewBackground(rh);
            oh.CreateNewPlate(450, 150, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 200, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 250, 30, 50, 1f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(450, 300, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 350, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 400, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(450, 450, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(400, 225, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(500, 225, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(350, 225, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(550, 225, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(500, 350, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(550, 350, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(600, 350, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(400, 350, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(350, 350, 30, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(300, 350, 30, 50, 1f, true, 0.00001f, true, false, true);

        }

        public void CreateLevelHostage(ObjectHandler oh,ResourceHandler rh){
            oh.CreateNewBackground(rh);
            oh.CreateNewPlate(200, 125, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(200, 155, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(140, 190, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(140, 160, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(260, 190, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(260, 160, 20, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewBobber(200, 200, 75, 30, 0, Math.PI/1.5, 0f, true, 0.000015f, false, false, false);
            oh.CreateNewBobber(200, 200, 75, 30, 0, -Math.PI/1.5, 0f, true, 0.000015f, false, false, false);
            oh.CreateNewBobber(200, 230, 75, 30, 0, -Math.PI/1.5, 0f, true, 0.000015f, false, false, false);
            oh.CreateNewBobber(200, 230, 75, 30, 0, Math.PI/1.5, 0f, true, 0.000015f, false, false, false);
            oh.CreateNewPopper(400, 250, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(600, 250, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(800, 250, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPlate(555, 225, 20, 200, 3f, true, 0.00001f, true, false, false);
            oh.CreateNewPlate(605, 225, 20, 200, 3f, true, 0.00001f, true, false, false);
            oh.CreateNewPlate(575, 215, 30, 200, 3f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(575, 250, 50, 200, 3f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(625, 215, 30, 200, 3f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(625, 250, 50, 200, 3f, true, 0.00001f, false, false, false);
            oh.CreateNewPlate(315, 340, 30, 15, 1f, true, 0.00001f, true, false, false);
            oh.CreateNewPlate(300, 335, 50, 200, 3f, true, 0.00001f, false, false, true);
            oh.CreateNewPlate(300, 380, 80, 200, 3f, true, 0.00001f, false, false, true);
            oh.CreateNewPlate(710, 380, 30, 200, 3f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(810, 380, 30, 200, 3f, true, 0.00001f, true, false, true);
            ObjectHandler.Popper po1=oh.CreateNewPopper(700, 380, 80, 0, 0, true, 0.00001f, true, false);
            ObjectHandler.Popper po2=oh.CreateNewPopper(800, 380, 80, 0, 0, true, 0.00001f, true, false);
            int p1=oh.CreateNewPlate(710, 380, 50, 30, 3f, false, 0.00001f, false, false, true).index;
            int p2=oh.CreateNewPlate(810, 380, 50, 30, -3f, false, 0.00001f, false, false, true).index;
            po1.AddConnection(ObjectHandler.PLATE, p1);
            po2.AddConnection(ObjectHandler.PLATE, p2);
            oh.CreateNewPlate(500, 350, 20, 200, 3f, true, 0.00001f, false, true, true);

            


        }

        public void CreateLevelDontStop(ObjectHandler oh,ResourceHandler rh){
            oh.CreateNewBackground(rh);
            int b1=oh.CreateNewBobber(700, 230, 100, 20, 0, -Math.PI/1.5, 0f, true, 0.00002f, true, false, true).index;
            int b2=oh.CreateNewBobber(700, 230, 100, 20, 0, Math.PI/1.5, 0f, true, 0.000015f, true, false, true).index;
            oh.CreateNewPlate(785, 175, 30, 50, 1f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(760, 145, 30, 50, 1f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(725, 135, 30, 50, 1f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(675, 135, 30, 50, 1f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(640, 145, 30, 50, 1f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(615, 175, 30, 50, 1f, true, 0.00001f, false, true, true);

            ObjectHandler.Popper p1=oh.CreateNewPopper(435, 250, 50, 75, 3f, true, 0.00001f, true, true);
            oh.CreateNewPlate(430, 210, 30, 75, -3f, true, 0.00001f, false, true, false);
            oh.CreateNewPlate(430, 235, 30, 75, -3f, true, 0.00001f, false, true, false);
            oh.CreateNewPlate(430, 260, 30, 75, -3f, true, 0.00001f, false, true, false);
            oh.CreateNewPopper(530, 250, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(330, 250, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(340, 250, 50, 0, 0, true, 0.00001f, false, false);
            oh.CreateNewPopper(370, 250, 50, 0, 0, true, 0.00001f, false, false);
            oh.CreateNewPopper(400, 250, 50, 0, 0, true, 0.00001f, false, false);
            oh.CreateNewPopper(430, 250, 50, 0, 0, true, 0.00001f, false, false);            
            oh.CreateNewPopper(460, 250, 50, 0, 0, true, 0.00001f, false, false);            
            oh.CreateNewPopper(490, 250, 50, 0, 0, true, 0.00001f, false, false);
            oh.CreateNewPopper(520, 250, 50, 0, 0, true, 0.00001f, false, false);
            p1.AddConnection(ObjectHandler.BOBBER, b1);
            p1.AddConnection(ObjectHandler.BOBBER, b2);

            oh.CreateNewPlate(305, 340, 20, 15, 1f, true, 0.00001f, true, false, false).AddConnection(ObjectHandler.POPPER, p1.index);
            oh.CreateNewPlate(300, 335, 50, 200, 3f, true, 0.00001f, false, true, true);
            oh.CreateNewPlate(300, 380, 80, 200, 3f, true, 0.00001f, false, true, true);

            oh.CreateNewPopper(100, 300, 50, 0, 0, true, 0.00001f, true, false);
            oh.CreateNewPopper(150, 300, 50, 0, 0, true, 0.00001f, true, false);

            oh.CreateNewPlate(480, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(530, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(580, 400, 35, 50, 1f, true, 0.00001f, true, false, true);
            oh.CreateNewPlate(630, 400, 35, 50, 1f, true, 0.00001f, true, false, true);

            oh.CreateNewTexan(200,200,100,20,50,0.0f,0f,0.00001f,true,true);

            
            

        }

        public void CreateLevelTakeCover(ObjectHandler oh,ResourceHandler rh){

        }

        public void InitGun(int levelNo,ObjectHandler oh){
            switch(levelNo){
                case DEATH_STAR:{
                    int[] buffer={19,19,19,19,19,19,19,19,19,19};
                    oh.gun.currentState=0;
                    oh.gun.nomag=false;
                    oh.gun.prevState=1;
                    oh.gun.numRds=14;
                    oh.gun.numMags=buffer;
                    break;
                }
                case RELOAD:{
                    int[] buffer={1,1,1,1,1,1,1,1,1,1};
                    oh.gun.currentState=0;
                    oh.gun.prevState=0;
                    oh.gun.nomag=false;
                    oh.gun.numRds=1;
                    oh.gun.numMags=buffer;
                    break;
                }
            }
        }

    }

 //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public class Timing{
        boolean running,ready,standby,gameStop;
        float[] shotTime;
        float currentTime;
        float penalty;
        int iterator,standbytime,shift;
        ObjectHandler oh;
        public Timing(ObjectHandler OH){
            running=false;
            ready=true;
            standby=false;
            shotTime=new float[1];
            shotTime[0]=0;
            penalty=0;
            currentTime=0;
            standbytime=30;
            gameStop=false;
            oh=OH;
        }

        public void ShiftScore(int amount){
            shift+=amount;
        }
        
        
        public void RefreshCurrentTime(){
            if(running){
            currentTime+=2;
            }else{
                currentTime=0;
                if(standby){
                    if(iterator<standbytime){
                    iterator+=1;
                    }else{
                        standby=false;
                        running=true;
                    }
                }
            }
        }
        public void Fired(){
            int newSize=shotTime.length+1;
            float[] buffer=new float[newSize];
            for(int i=0;i<shotTime.length;i++){
                buffer[i]=shotTime[i];
            }
            buffer[newSize-1]=currentTime;
            shotTime=buffer;
        }

        public void HitNoShoot(){
            penalty+=10;
        }
        public void HitStop(){
            running=false;
            penalty+=oh.CountAllAlive()*10;
            iterator=0;
            gameStop=true;
        }
        public void FinalTime(Graphics2D g){
            if(running){
                g.setColor(Color.GREEN);
                g.drawRect(500, 75, 30, 30);
            }else{
                g.setColor(Color.RED);
                g.drawRect(500, 75, 30, 30);
            }
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(18f));
            if(!running){
                for(int i=0;i<shotTime.length;i++){
                    g.drawString(String.valueOf(shotTime[i]/100f), 840, (i*20)+shift);
                }
                g.drawString("+"+String.valueOf(penalty), 770, 50);
                if(ready){
                    g.drawString("ready?(y)", 400, 75);                    
                }else{
                    if(standby){
                        g.drawString("stand by", 400, 75);                        
                    }
                }
            }

            if (gameStop){
                g.setColor(Color.WHITE);
                g.fillRect(300, 263, 370, 50);
                g.setColor(Color.BLACK);
                g.setFont(g.getFont().deriveFont(30f));

                g.drawString("Final Time:"+((Math.round(shotTime[shotTime.length-1]*100f)/10000f)+penalty)+"seconds", 300, 300);
            }
        }
    }

//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////

    public class Menu{
        static final short GAME=0;
        static final short MENU=1;
        static final short HELP=2;
        static final short PLAY=3;
        short state;
        short substate;
        int hX,hY;
        ResourceHandler resourceH;
        scButton buttonP,buttonH,buttonDeathStar,buttonTheCross,
                buttonReload,buttonLeadSprayer,buttonHostage,buttonBack,buttonDontStop;
        public Menu(ResourceHandler rh,ImageObserver obs){
            setCursor(Cursor.getDefaultCursor());
            resourceH=rh;
            state=MENU;
            substate=MENU;
            buttonP=new scButton(100,100,"Play",obs);
            buttonH=new scButton(100,450,"Instructions",obs);
            buttonDeathStar=new scButton(500,350,"DeathStar",obs);
            buttonTheCross=new scButton(500,100,"TheCross",obs);
            buttonReload=new scButton(500,150,"Reload",obs);
            buttonLeadSprayer=new scButton(500,200,"LeadSprayer",obs);
            buttonHostage=new scButton(500,250,"Hostage",obs);
            buttonDontStop=new scButton(500,300,"Don`t Stop",obs);
            buttonBack=new scButton(375,320,"Back",obs);
            hX=300;
            hY=50;

        }

 ///////////////////////////////////////////////////////////////////////////////////


        public class scButton{
            String buttonText;
            Rectangle2D.Float rect;
            int posX,posY;
            int wid,hei;
            boolean stateOn;
            public scButton(int x,int y,String text,ImageObserver obs){
                posX=x;
                posY=y;                
                wid=150;
                hei=25;
                rect=new Rectangle2D.Float(posX, posY, wid, hei);
                buttonText=text;
            }

            public void drawButton(Graphics2D g,ImageObserver obs){
                g.setStroke(new BasicStroke(0.1f));
                g.setColor(new Color(0,0,0,150));
                g.fillRect(posX+2, posY+2, wid, hei);
                if(stateOn){
                    g.setColor(new Color(152,197,245));
                    g.fillRect(posX, posY, wid, hei);
                }else{
                    g.setColor(Color.WHITE);
                    g.fillRect(posX, posY, wid, hei);
                }                
                g.setColor(Color.BLACK);
                g.drawRect(posX, posY, wid, hei);
                g.setFont(g.getFont().deriveFont(18f));
                g.drawString(buttonText, posX+5, posY+20);
            }

            public boolean TestContain(int x,int y){
                if(!rect.contains(x, y)){
                    stateOn=false;
                    return false;
                }else{
                    stateOn=true;
                    return true;
                }
            }
        }

   ////////////////////////////////////////////////////////////////////////////

        public void drawMenu(Graphics2D g,ImageObserver obs,Timing time){
            if(state==MENU){
                g.setColor(Color.BLACK);
                g.setFont(g.getFont().deriveFont(18f));
                g.drawString("Steel Challenge", 20, 50);
                g.setStroke(new BasicStroke(5f));
                g.drawOval(100, -100, 800, 800);
                buttonP.drawButton(g, obs);
                buttonH.drawButton(g, obs);
                switch (substate){
                    case PLAY:{
                        buttonTheCross.drawButton(g, obs);
                        buttonDeathStar.drawButton(g, obs);
                        buttonHostage.drawButton(g, obs);
                        buttonLeadSprayer.drawButton(g, obs);
                        buttonReload.drawButton(g, obs);
                        buttonDontStop.drawButton(g, obs);
                        break;
                    }
                    case HELP:{
                        g.drawImage(resourceH.helpPage, hX, hY, obs);
                        break;
                    }
                }
            }else{
                if(state==GAME&&time.gameStop){
                    buttonBack.drawButton(g, obs);
                }
            }
        }
    }

////////////////////////////////////////////////////////////////////////////
 //////////////////////////////////////////////////////////////////////////

   
    public class ResourceHandler implements Runnable{
        Image i1,i2,i3,i4,i5,i6,i7,barrel,house1,house2,bg,buttonPn,buttonPm,
                buttonHn,buttonHm,helpPage;
        AudioClip magin,magout,slideforward;
        AudioClip[] shot=new AudioClip[8];
        AudioClip[] shellbing=new AudioClip[3];
        boolean show=false;
        MediaTracker Tracker;
        BufferedImage usp0,usp2,shell,rel0,rel2,round,relhand;
        JPanel caller;
        
        //inditaskor nezze meg kesz van e betoltes
        //ha nincs inditson uj threadet
        public void start(){
            if (!Tracker.checkAll() ) {
                Thread thread = new Thread (this);
                thread.start ();
            } else{
                show = true;
            }
        }

       
        public void run ()  {
            try {
                Tracker.waitForAll();
            } catch (InterruptedException e) {}

            if (Tracker.isErrorAny()){
               JOptionPane.showMessageDialog(null, "Betoltes Sikertelen");
            }
            else{
                CreateOffscreenBuffer(caller);
            }

        }


        
        public ResourceHandler(JPanel panel){
            ClassLoader cl=SCmain.class.getClassLoader();
            i1=getImage(getCodeBase(),"usp0.png");
            i2=getImage(getCodeBase(),"usp2.png");
            i3=getImage(getCodeBase(),"case.png");
            i4=getImage(getCodeBase(),"reload0.png");
            i5=getImage(getCodeBase(),"reload2.png");
            i6=getImage(getCodeBase(),"round.png");
            i7=getImage(getCodeBase(),"handreload.png");
            bg=getImage(getCodeBase(),"bg.png");
            house1=getImage(getCodeBase(),"house.png");
            house2=getImage(getCodeBase(),"houseinner.png");
            barrel=getImage(getCodeBase(),"barrel.png");
            helpPage=getImage(getCodeBase(),"help.png");
            shot[0]=JApplet.newAudioClip(cl.getResource("shot1.wav"));
            shot[1]=JApplet.newAudioClip(cl.getResource("shot2.wav"));
            shot[2]=JApplet.newAudioClip(cl.getResource("shot3.wav"));
            shot[3]=JApplet.newAudioClip(cl.getResource("shot4.wav"));
            shot[4]=JApplet.newAudioClip(cl.getResource("shot5.wav"));
            shot[5]=JApplet.newAudioClip(cl.getResource("shot6.wav"));
            shot[6]=JApplet.newAudioClip(cl.getResource("shot7.wav"));
            shot[7]=JApplet.newAudioClip(cl.getResource("shot8.wav"));
            shellbing[0]=JApplet.newAudioClip(cl.getResource("shell1.wav"));
            shellbing[1]=JApplet.newAudioClip(cl.getResource("shell2.wav"));
            shellbing[2]=JApplet.newAudioClip(cl.getResource("shell3.wav"));
            magin=JApplet.newAudioClip(cl.getResource("maginsert.wav"));
            magout=JApplet.newAudioClip(cl.getResource("magrelease.wav"));
            slideforward=JApplet.newAudioClip(cl.getResource("slidein.wav"));
            Tracker=new MediaTracker(panel);
            Tracker.addImage(i1, 1);
            Tracker.addImage(i2, 2);
            Tracker.addImage(i3, 3);
            Tracker.addImage(i4, 4);
            Tracker.addImage(i5, 5);
            Tracker.addImage(i6, 6);
            Tracker.addImage(i7, 7);
            caller=panel;
        }

        
        public void CreateOffscreenBuffer(ImageObserver obs){
            usp0=new BufferedImage(874,554,BufferedImage.BITMASK);
            usp2=new BufferedImage(874,567,BufferedImage.BITMASK);
            shell=new BufferedImage(45,45,BufferedImage.BITMASK);
            rel0=new BufferedImage(582,649,BufferedImage.BITMASK);
            rel2=new BufferedImage(582,648,BufferedImage.BITMASK);
            round=new BufferedImage(50,50,BufferedImage.BITMASK);
            relhand=new BufferedImage(450,760,BufferedImage.BITMASK);
            Graphics2D osg=usp0.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i1, 0, 0, obs);
                    osg.setColor(Color.BLACK);
                    osg.dispose();

                    osg=usp2.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i2, 0, 0, obs);
                    osg.dispose();

                    osg=shell.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i3, 0, 0, obs);
                    osg.dispose();

                    osg=rel0.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i4, 0, 0, obs);
                    osg.dispose();

                    osg=rel2.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i5, 0, 0, obs);
                    osg.dispose();

                    osg=round.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i6, 0, 0, obs);
                    osg.dispose();

                    osg=relhand.createGraphics();
                    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    osg.drawImage(i7, 0, 0, obs);
                    osg.dispose();
        }
    }


    
    public class SCPanel extends JPanel implements MouseListener,MouseMotionListener,
                                                    KeyListener, FocusListener {
        
        boolean once;
        float time;
        Timer timer;
        ObjectHandler objects=new ObjectHandler();
        Levels lvl;
        ResourceHandler Resources;
        Timing timing=new Timing(objects);
        Menu menu;
        SCPanel pointer=this;
        

        public void keyTyped(KeyEvent evt) {}
        
        public void keyPressed(KeyEvent evt) {
            if(menu.state==menu.GAME){
                if(evt.getKeyChar()=='w'){
                    timing.ShiftScore(-10);
                }
                 if(evt.getKeyChar()=='s'){
                    timing.ShiftScore(10);
                }

                if(evt.getKeyChar()=='y'){
                    if(timing.ready){
                        timing.ready=false;
                        timing.standby=true;
                    }
                }


                if (objects.gun.isReloading){
                    //if(evt.isControlDown()){
                    if(evt.getKeyChar()=='1'){
                        objects.gun.SelectSlot(1,Resources);
                    }
                    if(evt.getKeyChar()=='2'){
                        objects.gun.SelectSlot(2,Resources);
                    }
                    if(evt.getKeyChar()=='3'){
                        objects.gun.SelectSlot(3,Resources);
                    }
                    if(evt.getKeyChar()=='4'){
                        objects.gun.SelectSlot(4,Resources);
                    }
                    if(evt.getKeyChar()=='5'){
                        objects.gun.SelectSlot(5,Resources);
                    }
                    if(evt.getKeyChar()=='6'){
                        objects.gun.SelectSlot(6,Resources);
                    }
                    if(evt.getKeyChar()=='7'){
                        objects.gun.SelectSlot(7,Resources);
                    }
                    if(evt.getKeyChar()=='8'){
                        objects.gun.SelectSlot(8,Resources);
                    }
                    if(evt.getKeyChar()=='9'){
                        objects.gun.SelectSlot(9,Resources);
                    }
                    if(evt.getKeyChar()=='0'){
                        objects.gun.SelectSlot(0,Resources);
                    }
                    //}
                }
                if(evt.getKeyChar()=='r'||evt.getKeyChar()=='R'){
                    if(!objects.gun.isReloading){
                    objects.gun.SetState(3);
                    }
                }
                if(evt.getKeyChar()=='e'||evt.getKeyChar()=='E'){                    
                    if(!objects.gun.nomag&&!objects.gun.isReloading){
                        objects.gun.SetState(objects.gun.NOMAG);
                        Resources.magout.play();
                    }
                }
            }else{
                if(menu.substate==menu.HELP){
                    if(evt.getKeyChar()=='w'){
                    menu.hY-=10;
                }
                 if(evt.getKeyChar()=='s'){
                    menu.hY+=10;
                }
                }
            }
        }
        public void keyReleased(KeyEvent evt) {}
        public void focusLost(FocusEvent evt) {}
        public void focusGained(FocusEvent evt) {}

        public void mouseEntered(MouseEvent evt) {
             requestFocus();
        }
         public void mouseExited(MouseEvent evt) { }
         public void mouseClicked(MouseEvent evt) {
             
         }
         
         public void mouseMoved(MouseEvent evt) {
             if(menu.state==menu.GAME&&timing.gameStop){
                 menu.buttonBack.TestContain(evt.getX(), evt.getY());
             }
             if(menu.state==menu.GAME){
                 objects.gun.RecalculateGunMouse(evt.getX(), evt.getY(),timing.standby,timing.ready);
             }else{
                 if(menu.state==menu.MENU){
                     menu.buttonH.TestContain(evt.getX(),evt.getY());
                     menu.buttonP.TestContain(evt.getX(),evt.getY());
                     menu.buttonLeadSprayer.TestContain(evt.getX(),evt.getY());
                     menu.buttonTheCross.TestContain(evt.getX(),evt.getY());
                     menu.buttonHostage.TestContain(evt.getX(),evt.getY());
                     menu.buttonReload.TestContain(evt.getX(),evt.getY());
                     menu.buttonDeathStar.TestContain(evt.getX(),evt.getY());
                     menu.buttonDontStop.TestContain(evt.getX(), evt.getY());
                 }
             }
         }
         public void mouseReleased(MouseEvent evt){}
         
         public void mousePressed(MouseEvent evt){
             if(menu.state==menu.GAME&&timing.gameStop){
                 if(menu.buttonBack.TestContain(evt.getX(), evt.getY())){
                     objects.oisi.discardAll();
                     menu.state=menu.MENU;
                     menu.substate=menu.MENU;
                     setCursor(Cursor.getDefaultCursor());
                 }
             }
             if(menu.state==menu.MENU){
                 if(menu.buttonP.TestContain(evt.getX(), evt.getY())){
                     menu.substate=menu.PLAY;
                 }
                 if(menu.buttonH.TestContain(evt.getX(), evt.getY())){
                     menu.substate=menu.HELP;
                 }
                 if(menu.buttonLeadSprayer.TestContain(evt.getX(), evt.getY())){
                     lvl=new Levels(Levels.LEAD_SPRAYER,objects,Resources);
                     objects.CreateGun(Resources);
                     lvl.InitGun(Levels.DEATH_STAR,objects);
                     setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
                     "cursor.png"), new Point(0,0), "cursor"));
                     timing.shotTime=new float[1];
                     timing.shotTime[0]=0;
                     timing.penalty=0.0f;
                     timing.ready=true;
                     timing.running=false;
                     timing.gameStop=false;
                     menu.state=menu.GAME;
                 }
                 if(menu.buttonReload.TestContain(evt.getX(), evt.getY())){
                     lvl=new Levels(Levels.RELOAD,objects,Resources);
                     objects.CreateGun(Resources);
                     lvl.InitGun(Levels.RELOAD,objects);
                     setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
                     "cursor.png"), new Point(0,0), "cursor"));
                     timing.shotTime=new float[1];
                     timing.shotTime[0]=0;
                     timing.penalty=0.0f;
                     timing.ready=true;
                     timing.running=false;
                     timing.gameStop=false;
                     menu.state=menu.GAME;
                 }
                 if(menu.buttonDontStop.TestContain(evt.getX(), evt.getY())){
                     lvl=new Levels(Levels.DONT_STOP,objects,Resources);
                     objects.CreateGun(Resources);
                     lvl.InitGun(Levels.DEATH_STAR,objects);
                     setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
                     "cursor.png"), new Point(0,0), "cursor"));
                     timing.shotTime=new float[1];
                     timing.shotTime[0]=0;
                     timing.penalty=0.0f;
                     timing.ready=true;
                     timing.running=false;
                     timing.gameStop=false;
                     menu.state=menu.GAME;
                 }
                 if(menu.buttonDeathStar.TestContain(evt.getX(), evt.getY())){
                     lvl=new Levels(Levels.DEATH_STAR,objects,Resources);
                     objects.CreateGun(Resources);
                     lvl.InitGun(Levels.DEATH_STAR,objects);
                     setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
                     "cursor.png"), new Point(0,0), "cursor"));
                     timing.shotTime=new float[1];
                     timing.shotTime[0]=0;
                     timing.penalty=0.0f;
                     timing.ready=true;
                     timing.running=false;
                     timing.gameStop=false;
                     menu.state=menu.GAME;
                 }
                 if(menu.buttonHostage.TestContain(evt.getX(), evt.getY())){
                     lvl=new Levels(Levels.HOSTAGE,objects,Resources);
                     objects.CreateGun(Resources);
                     lvl.InitGun(Levels.DEATH_STAR,objects);
                     setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
                     "cursor.png"), new Point(0,0), "cursor"));
                     timing.shotTime=new float[1];
                     timing.shotTime[0]=0;
                     timing.penalty=0.0f;
                     timing.ready=true;
                     timing.running=false;
                     timing.gameStop=false;
                     menu.state=menu.GAME;
                 }
                 if(menu.buttonTheCross.TestContain(evt.getX(), evt.getY())){
                     lvl=new Levels(Levels.THE_CROSS,objects,Resources);
                     objects.CreateGun(Resources);
                     lvl.InitGun(Levels.DEATH_STAR,objects);
                     setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
                     "cursor.png"), new Point(0,0), "cursor"));
                     timing.shotTime=new float[1];
                     timing.shotTime[0]=0;
                     timing.penalty=0.0f;
                     timing.ready=true;
                     timing.running=false;
                     timing.gameStop=false;
                     menu.state=menu.GAME;
                 }
             }
             if(menu.state==menu.GAME&&timing.running){
                 if(evt.getModifiersEx()==evt.BUTTON1_DOWN_MASK){
                 if(objects.gun.prevState==1&&objects.gun.currentState!=2){
                         objects.gun.SetState(1);
                         timing.Fired();
                         objects.TestHit(objects.gun.errX,objects.gun.errY,timing);
                         objects.CreateEjectShell(evt.getX(),evt.getY(),Resources,true);
                         int num=Math.round((float)(Math.random()*7));
                         Resources.shot[num].play();
                         num=Math.round((float)(Math.random()*2));
                         Resources.shellbing[num].play();

                 }
                 }
                 if(evt.getModifiersEx()==evt.BUTTON3_DOWN_MASK){
                     if((objects.gun.prevState==1||objects.gun.prevState==2)&&
                             objects.gun.currentState!=2){
                        objects.CreateEjectShell(evt.getX(), evt.getY(), Resources, false);
                    }
                    objects.gun.SetState(0);
                    Resources.slideforward.play();
                 }
             }

         }
         
         public void mouseDragged(MouseEvent evt){
             if(menu.state==menu.GAME){
             objects.gun.RecalculateGunMouse(evt.getX(), evt.getY(),timing.standby,timing.ready);
             }
         }

        
          private class TimerHandler implements ActionListener{
              public void actionPerformed(ActionEvent e){
                  if(menu.state==menu.GAME){
                  objects.CalculateAll();
                  timing.RefreshCurrentTime();
                  }
                 repaint();
                  
              }
          }

         
         public SCPanel(){
             setBackground(Color.WHITE);
             addMouseListener(this);
             addMouseMotionListener(this);
             addKeyListener(this);
             addFocusListener(this);
             Resources=new ResourceHandler(this);
             Thread thread=new Thread(Resources);
             thread.start();
             menu= new Menu(Resources,this);
             //lvl=new Levels(Levels.HOSTAGE,objects,Resources);
             //objects.CreateGun(Resources);
             //lvl.InitGun(Levels.DEATH_STAR,objects);
             timer=new Timer(20,new TimerHandler());
             timer.start();
             //setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage(getCodeBase(),
             //           "cursor.png"), new Point(0,0), "cursor"));
             
         }

         
         public void paintComponent(Graphics g){             
             super.paintComponent(g);
             Graphics2D g2=(Graphics2D)g;
             g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
             g2.drawRect(0, 0, 900, 600);
             if(menu.state==menu.GAME){
             objects.AnimateAll(g2,this);
             timing.FinalTime(g2);
             }   
             menu.drawMenu(g2,this,timing);

        }
    }
}
