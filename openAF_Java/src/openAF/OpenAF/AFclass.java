//Copyright 2026 Imperial College London
//Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
//1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
//
//2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 /**
 *
 * @author jpelightley
 *
 */

package openAF.OpenAF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Math.round;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.jfree.ui.RefineryUtilities;
import org.joda.time.DateTime;

public class AFclass {
    AFlogic aflog_ = new AFlogic();
    MainAF parent_ = null;
    private CMMCore core_;
    Scanner sc; 
    double afFoc = 1;
    double afFoc2 = 1;
    double afFoc3 = 1;
    double realFoc = 0;
    String zDev = null;
    long mody = 0;
    ArrayList<Double> afList = new ArrayList<Double>();
    ArrayList<Double> afList2 = new ArrayList<Double>();
    ArrayList<Double> afList3 = new ArrayList<Double>();
    ArrayList<Double> reList = new ArrayList<Double>();
    ArrayList<Double> xList = new ArrayList<Double>();
    double rangeZ = 20; // umeter
    double incZ = 0.2;    // umater
    double lowerLim = 1278;
    double upperLim = 1298;
    double linear_range = 20;
    double currZ = 0;
    double current_z = 9000;
    double defined_focus=0;
    double next_z = 90001;
    double new_z_pos = 9000;
    double current_metric_val = 0;
    double current_metric_val_coarse = 0;
    double current_int = 0;
    double current_rad_2b = 0;
    double current_int2 = 0;
    double start_pos = 9000;
    double end_pos = 9001;
    ArrayList<Double> stepSizeList = new ArrayList<Double>();
    double repRange = 5;
    double repSteps = 0.25;
    double repMax = 1;
    double dither = 0.1;
    double serStart = 50;
    double serEnd = 250;
    double serStep = 50;
    double fineRange = 6;
    double fineSteps = 0.2;
    int rep = 5;
    int ccc =0;
    boolean dp = false;
    double calibMax = 1350;
    double calibMin = 1220;
    ArrayList<Double> LUT1 = new ArrayList<Double>();
    ArrayList<Double> LUT2 = new ArrayList<Double>();
    ArrayList<Double> LUT3 = new ArrayList<Double>();
    ArrayList<Double> calibList = new ArrayList<Double>();
    int countH;
    PrintWriter Z_out;
        
    boolean interp_bool = false;
    boolean disable_bool = false;
    
    static Thread sent;
    static Thread receive;
    static Socket socket;
    static Thread buttonListner;
    static Thread backgroundSent;
    static Thread backgroundReceive;
    
    //String path = "C:\\Program Files\\Micro-Manager-2.0gamma\\zPosSTORM.txt";   
    String path = null;   
    File mm_dir = null;
    String OAF2_path = null;
    String defined_list = "upper";
    String time_prefix = null;
    
    ArrayList<Double> lookuplist_rad = new ArrayList<Double>();
    ArrayList<Double> lookuplist_z = new ArrayList<Double>();
    
    AFclass(MainAF parent_in){
        parent_ = parent_in;
        socket = parent_.getSocket();
        zDev = parent_.core_.getFocusDevice();
//        String path_z_file = "S:\\2025\\"+parent_.time_prefix+"Zlist2.txt";
//        try {
//            Z_out = new PrintWriter(path_z_file);
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
//        }
        mm_dir = parent_.get_mm_dir();
        time_prefix = DateTime.now().toString("YYYY-MM-dd_HH-mm-ss");
        OAF2_path = mm_dir.toString()+File.separator+Common_references.OAF_subfolder+File.separator+time_prefix;
        //path = mm_dir.toString()+"zPosSTORM.txt";
        try {
            Files.createDirectories(Paths.get(OAF2_path));
            String path_z_file = OAF2_path+File.separator+time_prefix+"_"+Common_references.Z_logfile_name;
            Z_out = new PrintWriter(path_z_file);
        } catch (IOException ex) {
            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void set_interp_status(boolean status_to_set){
        interp_bool = status_to_set;
    }
    
    public boolean get_interp_status(boolean status_to_set){
        return interp_bool;
    }    
    
    public void set_z_disable_status(boolean status_to_set){
        disable_bool = status_to_set;
    }
    
    public boolean get_z_disable_status(){
        return disable_bool;
    }
    
    public void defineAFFocus_Proj() {
        Double target_fine_value = null;
        Double target_coarse_value = null;
        try {
            defined_focus=parent_.core_.getPosition(zDev);
            current_z = parent_.core_.getPosition(zDev);
        } catch (Exception ex) {
            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(dp != true){
            aflog_.split_file_Proj(afList, afList2, reList, afList3, parent_.Intensity_threshold);
        }
        boolean focusing = true;
        while(focusing){
            String hh = "0";
            synchronized(parent_.control_){
                parent_.control_.flagRead=true;
                parent_.control_.flagSent=true;
                boolean cc = true;
                while(cc){
                    if(parent_.control_.flagRead){
                        hh = parent_.control_.pyZ;
                        cc = false;
                        parent_.control_.flagRead = false;
                    }
                }
            }
            boolean co = true;
            while(co){
                if(hh!=parent_.control_.pyZ){
                    current_metric_val = Double.parseDouble(parent_.control_.pyZ);
                    co=false;
                }
            }
            focusing = false;
        }
        current_metric_val = Double.parseDouble(parent_.control_.pyZ);
        current_metric_val_coarse = Double.parseDouble(parent_.control_.pyZ2);
        current_int = Double.parseDouble(parent_.control_.avgInt);
        afFoc  = Double.parseDouble(parent_.control_.pyZ);
        afFoc2  = Double.parseDouble(parent_.control_.pyZ2);  
        afFoc3 = Double.parseDouble(parent_.control_.avgInt);
        
        String current_low_fine_pos = aflog_.look_up_defocus(current_metric_val, aflog_.lower_half_fine_proj_list, aflog_.lower_half_fine_z_list,interp_bool);
        String current_high_fine_pos = aflog_.look_up_defocus(current_metric_val, aflog_.upper_half_fine_proj_list, aflog_.upper_half_fine_z_list,interp_bool);
        int coarse_lower_idx = 0;
        int coarse_upper_idx = 0;
        ArrayList<Double> coarse_lower_list = null;
        ArrayList<Double> coarse_upper_list = null;
        if(aflog_.lower_half_coarse_z_list.contains(Double.parseDouble(current_low_fine_pos))){
            coarse_lower_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_low_fine_pos), aflog_.lower_half_coarse_z_list);
            coarse_lower_list = aflog_.lower_half_coarse_proj_list;
        } else if(aflog_.upper_half_coarse_z_list.contains(Double.parseDouble(current_low_fine_pos))){
            coarse_lower_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_low_fine_pos), aflog_.upper_half_coarse_z_list);
            coarse_lower_list = aflog_.upper_half_coarse_proj_list;
        }
        
        if(aflog_.lower_half_coarse_z_list.contains(Double.parseDouble(current_high_fine_pos))){
            coarse_upper_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_high_fine_pos), aflog_.lower_half_coarse_z_list);
            coarse_upper_list = aflog_.lower_half_coarse_proj_list;
        } else if(aflog_.upper_half_coarse_z_list.contains(Double.parseDouble(current_high_fine_pos))){
            coarse_upper_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_high_fine_pos), aflog_.upper_half_coarse_z_list);
            coarse_upper_list = aflog_.upper_half_coarse_proj_list;
        }

        Double target_low_coarse_value = coarse_lower_list.get(coarse_lower_idx);
        Double target_high_coarse_value = coarse_upper_list.get(coarse_upper_idx);

        if(current_metric_val >= parent_.Rad_threshold){
            if(Math.abs(current_metric_val_coarse-target_low_coarse_value) < Math.abs(current_metric_val_coarse-target_high_coarse_value)){         
                defined_list = "lower";                
            }
            else{
                defined_list = "upper";
            }           
        }   
    }
    
    public double defineAFFocus() throws InterruptedException {
        if(dp != true){
            try {
                aflog_.read_file(path);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        String hh = "0";
        synchronized(parent_.control_){
            parent_.control_.flagRead=true;
            parent_.control_.flagSent=true;
            boolean cc = true;           
            while(cc){
                if(parent_.control_.flagRead){
                    hh = parent_.control_.pyZ;
                    cc = false;
                    parent_.control_.flagRead = false;
                }
            }
        }
        boolean co = true;
        while(co){
            if(hh!=parent_.control_.pyZ){
                afFoc = Double.parseDouble(parent_.control_.pyZ);
                co=false;
            }
        }
        afFoc = Double.parseDouble(parent_.control_.pyZ);
        System.out.println(afFoc);
        return afFoc; 
    }
    
    public String setZDev(){
        zDev = parent_.core_.getFocusDevice();
        return zDev;
    }
    
    public void calib(boolean dia){
        reList.clear();
        afList.clear();
        afList2.clear();
        afList3.clear();
        long stepsZ = round(2*parent_.range_/parent_.stepSize_);
        double af1 = 0;
        double af2 = 0;
        double af3 = 0;
        double re1 = 0; 
        double currZpreCalib = 3000;
        // set current position to start of scan range
        try{
            currZ = parent_.core_.getPosition(zDev);
            currZpreCalib = currZ;
            re1 = currZ-parent_.range_-parent_.stepSize_;
            parent_.core_.setPosition(zDev, re1);
            parent_.core_.waitForDevice(zDev);
            Thread.sleep(1000);
        } catch(Exception ex){
                System.out.println("Skipped z device outside loop");
            }
        //swipe thorgh z planes and read txt file
        synchronized(parent_.control_){parent_.control_.flagRead=true;}
        for (int i = 0; i <= stepsZ; i++) {
            re1 = re1+parent_.stepSize_;
            reList.add(re1);
            try{
                parent_.core_.setPosition(zDev, re1);
                parent_.core_.waitForDevice(zDev);
                currZ = re1;
            } catch(Exception ex){
                System.out.println("Skipped z device in loop");
            }
            boolean cc = true;
            String hh = "0";
            synchronized(parent_.control_){
                while(cc){
                    if(parent_.control_.flagRead){
                        parent_.control_.flagSent = true;
                        parent_.control_.flagRead = false;
                        cc = false;
                        hh = parent_.control_.pyZ;
                    }else{
                        System.out.println("wait for message");
                    }    
                }
            }
            boolean co = true;
            while(co){
                if(hh!=parent_.control_.pyZ){
                    af1 = Double.parseDouble(parent_.control_.pyZ);
                    af2 = Double.parseDouble(parent_.control_.pyZ2);
                    af3 = Double.parseDouble(parent_.control_.avgInt);
                    co=false;
                }
            }
            afList.add(af1);
            afList2.add(af2);
            afList3.add(af3);
        }
        System.out.println(afList);
        System.out.println(reList);
        
        // prepare data for diagram
        int l = afList.size();
        double[][] a = new double[l][4];
        for (int ii = 0; ii < l-1; ii++) {  
            a[ii][0] = reList.get(ii);
            a[ii][1] = afList.get(ii);
            a[ii][2] = afList2.get(ii);
            a[ii][3] = afList3.get(ii);
        }
        
        // show diagram
        if(dia){
            final diagram demo = new diagram(a, parent_);
            demo.pack();
            RefineryUtilities.centerFrameOnScreen(demo);
            demo.setVisible(true);
        }

        // move to starting position
        try {
            parent_.core_.waitForDevice(zDev);
            parent_.core_.setPosition(zDev, currZpreCalib);
            parent_.core_.waitForDevice(zDev);
            Thread.sleep(2000);
        } catch (Exception ex) {
            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void noise_background(boolean dia){
        double af1 = 0;
        synchronized(parent_.control_){
            parent_.control_.flagRead=true;
        }
        boolean cc = true;
        String hh = "0";
        synchronized(parent_.control_){
            while(cc){
                if(parent_.control_.flagRead){
                    parent_.control_.flagNoiseBackgroundSent =true;   
                    parent_.control_.flagRead = false;
                    cc = false;
                    hh = parent_.control_.back;
                }else{
                    System.out.println("wait for message");
                }    
            }
        }
        boolean co = true;
        while(co){
            if(hh!=parent_.control_.back){
                af1 = Double.parseDouble(parent_.control_.back);
                co=false;
            }
        }
    }
    
    public void background_aboveF(boolean dia) {
        double af1 = 0;
        synchronized(parent_.control_){
            parent_.control_.flagRead=true;
        }
        boolean cc = true;
        String hh = "0";
        synchronized(parent_.control_){
            while(cc){
                if(parent_.control_.flagRead){
                    parent_.control_.flagBackgroundAboveFSent =true;   
                    parent_.control_.flagRead = false;
                    cc = false;
                    hh = parent_.control_.back;
                }else{
                    System.out.println("wait for message");
                }    
            }
        }
        boolean co = true;
        while(co){
            if(hh!=parent_.control_.back){
                af1 = Double.parseDouble(parent_.control_.back);
                co=false;
            }
        }
    }    
    
    public void infocus_signal(boolean dia){
        double af1 = 0;
        synchronized(parent_.control_){
            parent_.control_.flagRead=true;
        }
        boolean cc = true;
        String hh = "0";
        synchronized(parent_.control_){
            while(cc){
                if(parent_.control_.flagRead){
                    parent_.control_.flagInfocusSent =true;   
                    parent_.control_.flagRead = false;
                    cc = false;
                    hh = parent_.control_.back;
                }else{
                    System.out.println("wait for message");
                }    
            }
        }
        boolean co = true;
        while(co){
            if(hh!=parent_.control_.back){
                af1 = Double.parseDouble(parent_.control_.back);
                co=false;
            }
        }
    } 
    
    public void setpath_signal(boolean dia){
        double af1 = 0;
        synchronized(parent_.control_){
            parent_.control_.flagRead=true;
        }
        boolean cc = true;
        String hh = "0";
        synchronized(parent_.control_){
            while(cc){
                if(parent_.control_.flagRead){
                    parent_.control_.flagSetPathSent = true;   
                    parent_.control_.flagRead = false;
                    cc = false;
                    hh = parent_.control_.back;
                }else{
                    System.out.println("wait for message");
                }    
            }
        }
        boolean co = true;
        while(co){
            if(hh!=parent_.control_.back){
                af1 = Double.parseDouble(parent_.control_.back);
                co=false;
            }
        }
    }     
    
    public void background(boolean dia){
        long stepsZ = round(2*parent_.range_/parent_.stepSize_);
        double af1 = 0;
        double re1 = 0; 
        double currZpreCalib = 3000;
        // set current position to start of scan range
        try{
            currZ = parent_.core_.getPosition(zDev);
            currZpreCalib = currZ;
            re1 = currZ-parent_.range_-parent_.stepSize_;
            parent_.core_.setPosition(zDev, re1);
            parent_.core_.waitForDevice(zDev);
            Thread.sleep(1000);
            //currZ = core_.getPosition(zDev);
        }catch(Exception ex){
            System.out.println("Skipped z device outside loop");
        }
        //swipe thorgh z planes and read txt file
        synchronized(parent_.control_){parent_.control_.flagRead=true;}
        for (int i = 0; i <= stepsZ; i++) {
            re1 = re1+parent_.stepSize_;
            try{
                parent_.core_.setPosition(zDev, re1);
                parent_.core_.waitForDevice(zDev);
                currZ = re1;
            }catch(Exception ex){
                System.out.println("Skipped z device in loop");
            }
            boolean cc = true;
            String hh = "0";
            synchronized(parent_.control_){
                while(cc){
                    if(parent_.control_.flagRead){
                        if(i==0){
                            parent_.control_.flagBackgroundFirstSent =true;   
                        } else {
                            parent_.control_.flagBackgroundSent = true;
                        }
                        parent_.control_.flagRead = false;
                        cc = false;
                        hh = parent_.control_.back;
                    } else {
                        System.out.println("wait for message");
                    }    
                }
            }
            boolean co = true;
            while(co){
                if(hh!=parent_.control_.back){
                    af1 = Double.parseDouble(parent_.control_.back);
                    co=false;
                }
            }
        }
        // move to starting position
        try {
            parent_.core_.setPosition(zDev, currZpreCalib);
            parent_.core_.waitForDevice(zDev);
            Thread.sleep(1000);
        } catch (Exception ex) {
            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void goToFocus_Projections() {
        if(dp != true){
            aflog_.split_file_Proj(afList, afList2, reList, afList3, parent_.Intensity_threshold);
        }
        boolean focusing = true;
        int repeats_ = 0;
        while(focusing){
            String hh = "0";
            synchronized(parent_.control_){
                parent_.control_.flagRead=true;
                parent_.control_.flagSent=true;
                boolean cc = true;
                while(cc){
                    if(parent_.control_.flagRead){
                        hh = parent_.control_.pyZ;
                        cc = false;
                        parent_.control_.flagRead = false;
                    }
                }
            }
            boolean co = true;
            while(co){
                if(hh!=parent_.control_.pyZ){
                    current_metric_val = Double.parseDouble(parent_.control_.pyZ);
                    co=false;
                }
            }
            current_metric_val = Double.parseDouble(parent_.control_.pyZ);
            current_metric_val_coarse = Double.parseDouble(parent_.control_.pyZ2);
            current_int = Double.parseDouble(parent_.control_.avgInt);

            if(dp == true){
                try {
                    current_z = parent_.core_.getPosition(zDev);
                    double defocus = (Math.round(100*((afFoc - Double.parseDouble(parent_.control_.pyZ)))))/100.0;
                    System.out.println(defocus);
                    new_z_pos = current_z + defocus;
                    System.out.println(new_z_pos);
                    current_z = Math.round(new_z_pos*100.0)/100.0;
                    System.out.println(current_z);
                    parent_.core_.setPosition(zDev, current_z);
                    parent_.core_.waitForDevice(zDev);
                    focusing =false;
                } catch (Exception ex) {
                    Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if(current_int > parent_.Intensity_threshold){
                    String current_pos = null;
                    String current_low_coarse_pos = null;
                    String current_high_coarse_pos = null;
                    Double target_low_coarse_value = null;
                    Double target_high_coarse_value = null;                    
                    String current_low_fine_pos = aflog_.look_up_defocus(current_metric_val, aflog_.lower_half_fine_proj_list, aflog_.lower_half_fine_z_list,false);
                    String current_high_fine_pos = aflog_.look_up_defocus(current_metric_val, aflog_.upper_half_fine_proj_list, aflog_.upper_half_fine_z_list,false);                  
                    int coarse_lower_idx = 0;
                    int coarse_upper_idx = 0;
                    ArrayList<Double> coarse_lower_list = null;
                    ArrayList<Double> coarse_upper_list = null;
                    if(aflog_.lower_half_coarse_z_list.contains(Double.parseDouble(current_low_fine_pos))){
                        coarse_lower_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_low_fine_pos), aflog_.lower_half_coarse_z_list);
                        coarse_lower_list = aflog_.lower_half_coarse_proj_list;
                    }
                    else if(aflog_.upper_half_coarse_z_list.contains(Double.parseDouble(current_low_fine_pos))){
                        coarse_lower_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_low_fine_pos), aflog_.upper_half_coarse_z_list);
                        coarse_lower_list = aflog_.upper_half_coarse_proj_list;
                    }
                    if(aflog_.lower_half_coarse_z_list.contains(Double.parseDouble(current_high_fine_pos))){
                        coarse_upper_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_high_fine_pos), aflog_.lower_half_coarse_z_list);
                        coarse_upper_list = aflog_.lower_half_coarse_proj_list;
                    }
                    else if(aflog_.upper_half_coarse_z_list.contains(Double.parseDouble(current_high_fine_pos))){
                        coarse_upper_idx = aflog_.look_up_closest_index_from_Z(Double.parseDouble(current_high_fine_pos), aflog_.upper_half_coarse_z_list);
                        coarse_upper_list = aflog_.upper_half_coarse_proj_list;;
                    }
                    target_low_coarse_value = coarse_lower_list.get(coarse_lower_idx);
                    target_high_coarse_value = coarse_upper_list.get(coarse_upper_idx);
                    if(current_metric_val >= parent_.Rad_threshold){
                        if(Math.abs(current_metric_val_coarse-target_low_coarse_value) < Math.abs(current_metric_val_coarse-target_high_coarse_value)){         
                            current_pos = aflog_.look_up_defocus(current_metric_val, aflog_.lower_half_fine_proj_list, aflog_.lower_half_fine_z_list,interp_bool);
                        } else {
                            current_pos = aflog_.look_up_defocus(current_metric_val, aflog_.upper_half_fine_proj_list, aflog_.upper_half_fine_z_list,interp_bool);
                        }

                        if(defined_list == "upper"){
                            lookuplist_rad = aflog_.upper_half_fine_proj_list;
                            lookuplist_z = aflog_.upper_half_fine_z_list;
                        } else {
                            lookuplist_rad = aflog_.lower_half_fine_proj_list;
                            lookuplist_z = aflog_.lower_half_fine_z_list;
                        }
                        String Focus_pos = aflog_.look_up_defocus(afFoc, lookuplist_rad,lookuplist_z,interp_bool);
                        double defocus = (Math.round(100*((Double.parseDouble(Focus_pos) - (Double.parseDouble(current_pos))))))/100.0;
                        System.out.println("defocus = " + defocus);
                        try {
                            current_z = parent_.core_.getPosition(zDev);
                        } catch (Exception ex) {
                            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if(!disable_bool){
                            new_z_pos = current_z + defocus;
                        } else {
                            new_z_pos = current_z;
                        }
                        current_z = Math.round(new_z_pos*1000.0)/1000.0;
                        parent_.lastFocusValue = current_z;
                        repeats_ = 0;
                        System.out.println(current_z);
                        LocalDateTime timestamp = LocalDateTime.now();
                        String disabled = "Z_ON";
                        if(disable_bool){
                            disabled = "Z_OFF";
                        }
                        String text = disabled + "," + timestamp.toString() +"," + String.valueOf(defocus) + "," + String.valueOf(current_z); 
                        Z_out.println(text);
                        try {
                            Z_out.println("AF threshold is set higher than values being reported from the AF unit! Motion disabled");
                            if(!disable_bool){
                                System.out.println("Z active!!!!");
                                parent_.core_.setPosition(zDev, current_z);
                                parent_.core_.waitForDevice(zDev);
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        focusing =false;
                    } else {
                        try {
                            if(!disable_bool){       
                                parent_.core_.setPosition(zDev, defined_focus);
                                parent_.core_.waitForDevice(zDev);
                            }
                            focusing = false; 
                        } catch (Exception ex) {
                            Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {             
                    try {
                        if(!disable_bool){
                            parent_.core_.setPosition(zDev, defined_focus); //parent_.lastFocusValue
                            parent_.core_.waitForDevice(zDev);
                        }
                        focusing = false; 
                    } catch (Exception ex) {
                    Logger.getLogger(AFclass.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
} 