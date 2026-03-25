# -*- coding: utf-8 -*-
"""
Created on Thu Aug  3 16:52:44 2023

@author: Jonathan Lightley

Copyright 2023 Imperial College London
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this 
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
"""

import numpy as np
import PySpin as pyspin
import cv2
import scipy.ndimage
import scipy.signal
from scipy.interpolate import UnivariateSpline
import time
import matplotlib.pyplot as plt

from datetime import datetime

class autofocus():

    def __init__(self):
        self.system = pyspin.System.GetInstance()
        self.cam_list = self.system.GetCameras()
        self.num_cameras = self.cam_list.GetSize()
        for i in range(self.num_cameras):
            self.cam = self.cam_list.GetByIndex(i)
            if self.cam.TLDevice.DeviceDisplayName.ToString() == "Point Grey Research Chameleon3 CM3-U3-31S4M":
                break
        self.bg_list = []
        self.background = np.zeros((768,1024)) 
        self.noise_background =np.zeros((768,1024)) 
        self.camera_setup()
        self.image_width = 0
        self.image_width = 0
        self.csv_num = 0
        self.proj_list = []
        self.proj1_list = []
        self.image_list = []
        self.bg_corr_mask =np.zeros((768,1024)) 
        self.infocus_image = np.zeros((768,1024)) 
        self.non_focal_power_initial = None
        self.power_initial = None
        self.power_current = None
        self.BGC_set_bg_correction_ROI()
        self.ratio_NFP_FP_initial = None
        self.FP_NFP_R_L=[]
        self.AVGI_MB_inital=None
        self.tot_Avg_now=None
        self.AVGI_MN_inital=None
        self.AVGIX_now=None
        self.Avg_BR=None
        self.Avg_BS=None
        self.background_aboveF=np.zeros((768,1024)) 
        
    def camera_setup(self):

        self.cam.Init()
        if self.cam.ExposureAuto.GetAccessMode() != pyspin.RW:
            print("Unable to disable automatic exposure. Aborting...")
        self.cam.ExposureAuto.SetValue(pyspin.ExposureAuto_Off)
        exposure_time_to_set = 8000#45000#
        exposure_time_to_set = min(self.cam.ExposureTime.GetMax(), exposure_time_to_set)
        self.cam.ExposureTime.SetValue(exposure_time_to_set)
        self.cam.GainAuto.SetValue(pyspin.GainAuto_Off)
        self.cam.Gain.SetValue(0.0)
        nodemap_tldevice = self.cam.GetTLDeviceNodeMap()
        nodemap = self.cam.GetNodeMap()
        node_acquisition_mode = pyspin.CEnumerationPtr(nodemap.GetNode("AcquisitionMode"))
        node_acquisition_mode_continuous = node_acquisition_mode.GetEntryByName("Continuous")
        acquisition_mode_continuous = node_acquisition_mode_continuous.GetValue()
        node_acquisition_mode.SetIntValue(acquisition_mode_continuous)
        node_device_serial_number = pyspin.CStringPtr(nodemap_tldevice.GetNode("DeviceSerialNumber"))
        device_serial_number = node_device_serial_number.GetValue()
        print('Starting data acquisition...')


    def camera_close(self):
        print('Stopping acquisition...')
        self.cam.DeInit()
        print('Closing first camera...')

    def set_background(self, reset_background):
        if reset_background==True:
           self.bg_list = []
           self.background = np.zeros((1536,2048))
        self.cam.BeginAcquisition()
        bg_image = self.cam.GetNextImage()
        if self.image_width == 0 or self.image_height == 0:
            self.image_width = bg_image.GetWidth()
            self.image_height = bg_image.GetHeight()
        bg_image_data = np.array(bg_image.GetData(), dtype="uint16").reshape( (self.image_height, self.image_width))
        self.bg_list.append(bg_image_data)
        self.background = np.mean(self.bg_list,axis = 0)
        print(self.background)
        bg_image.Release()
        self.cam.EndAcquisition()
        
    def set_background_aboveF(self):
        self.cam.BeginAcquisition()
        bg_image = self.cam.GetNextImage()
        if self.image_width == 0 or self.image_height == 0:
            self.image_width = bg_image.GetWidth()
            self.image_height = bg_image.GetHeight()
        bg_image_data = np.array(bg_image.GetData(), dtype="uint16").reshape( (self.image_height, self.image_width))
        self.background_aboveF = bg_image_data
        print(self.background_aboveF)
        bg_image.Release()
        self.cam.EndAcquisition()
        
        
        

    def set_noise_background(self):
        self.cam.BeginAcquisition()
        bg_image = self.cam.GetNextImage()
        if self.image_width == 0 or self.image_height == 0:
            self.image_width = bg_image.GetWidth()
            self.image_height = bg_image.GetHeight()
        bg_image_data = np.array(bg_image.GetData(), dtype="uint16").reshape( (self.image_height, self.image_width))
        self.noise_background = bg_image_data
        print(self.noise_background)
        bg_image.Release()
        self.cam.EndAcquisition()
        
    def BGC_set_infocus_image(self):
        self.cam.BeginAcquisition()
        bg_image = self.cam.GetNextImage()
        if self.image_width == 0 or self.image_height == 0:
            self.image_width = bg_image.GetWidth()
            self.image_height = bg_image.GetHeight()
        bg_image_data = np.array(bg_image.GetData(), dtype="uint16").reshape( (self.image_height, self.image_width))
        self.infocus_image  = bg_image_data
        noise_bg_32 = self.noise_background.astype(np.int32)

        bg_image.Release()
        
        self.cam.EndAcquisition()
        
    def BGC_set_bg_correction_ROI(self):
        #Setup the mask used for background correction
        edge_fraction = 0.1
        x_px = self.bg_corr_mask.shape[0]
        y_px = self.bg_corr_mask.shape[1]
        x_min, x_max = int((x_px)*(edge_fraction)),int((x_px)*(1-edge_fraction))
        y_min, y_max = int((y_px)*(edge_fraction)),int((y_px)*(1-edge_fraction))
        
        self.bg_corr_mask[x_min:x_max, y_min:y_max] = 1
 
   
    def calc_std(self):
        #power_fraction,self.power_current,non_focal_power_now,ratio_NFP_FP_now,avg_int,Avg_MBI = 1,1,1,1,1,1
        
        self.cam.BeginAcquisition()
        image_result = self.cam.GetNextImage()
        if self.image_width == 0 or self.image_height == 0:
            self.image_width = image_result.GetWidth()
            self.image_height = image_result.GetHeight()
        image_data = np.array(image_result.GetData(), dtype="uint16").reshape( (self.image_height, self.image_width))
        image_data32 = image_data.astype(np.float32)#.astype(np.int32)
        
        if(np.mean(self.noise_background)<0):
            print("Noise background not yet set!")
            print('\r\n')
        if(np.mean(self.infocus_image)<0):
            print("Reference image not yet set!")
            print('\r\n')
            
        image_height,image_width=self.image_height,self.image_width
        size_height= int(1*image_height) 
        size_width=int(1*image_width) 
       
        bg_32 = self.background.astype(np.float32) 
        bgAF_32=self.background_aboveF.astype(np.float32)
        
        
        noise_bg_32 = self.noise_background.astype(np.float32) 
        bg_32 = np.subtract(bg_32, noise_bg_32)
        
       
        image_data32 = np.subtract(image_data32, noise_bg_32)
        avg_int=np.mean(image_data32)
        image_data32 = cv2.GaussianBlur(image_data32, (41,41), cv2.BORDER_DEFAULT)
        
        
        image_data32_cropped = image_data32[int(image_height/2)-size_height:int(image_height/2)+size_height, int(image_width/2)-size_width:int(image_width/2)+size_width]#[int(image_height/2)-int(image_height/4):int(image_height/2)+int(image_height/4), int(image_width/2)-int(image_width/4):int(image_width/2)+int(image_width/4)]#[int(center/2)-int(center/4):int(center/2)+int(center/4), int(center/2)-int(center/4):int(center/2)+int(center/4)]
        bg_32 = bg_32[int(image_height/2)-size_height:int(image_height/2)+size_height, int(image_width/2)-size_width:int(image_width/2)+size_width]#[int(image_height/2)-int(image_height/4):int(image_height/2)+int(image_height/4), int(image_width/2)-int(image_width/4):int(image_width/2)+int(image_width/4)]#[int(center/2)-int(center/4):int(center/2)+int(center/4), int(center/2)-int(center/4):int(center/2)+int(center/4)]

       


        in_focusimg_32 =  self.infocus_image.astype(np.float32)#.astype(np.int32)
        in_focusimg_32 = np.subtract(in_focusimg_32, noise_bg_32)
        in_focusimg_32 = cv2.GaussianBlur(in_focusimg_32, (41,41), cv2.BORDER_DEFAULT)
        in_focusimg_32_cropped = in_focusimg_32[int(image_height/2)-size_height:int(image_height/2)+size_height, int(image_width/2)-size_width:int(image_width/2)+size_width]#[int(image_height/2)-int(image_height/4):int(image_height/2)+int(image_height/4), int(image_width/2)-int(image_width/4):int(image_width/2)+int(image_width/4)]#[int(center/2)-int(center/4):int(center/2)+int(center/4), int(center/2)-int(center/4):int(center/2)+int(center/4)]
       
        
       
    
    
 
        
    
        in_focusimg_32 = in_focusimg_32.astype(np.int32)
        
     
        
        MP0_infocus_cropped = np.mean(in_focusimg_32_cropped, axis=0) #* x_mask
        MP1_infocus_cropped = np.mean(in_focusimg_32_cropped, axis=1) #* y_mask
        
    
        
        MP0_image_cropped = np.mean(image_data32_cropped, axis=0) #* x_mask
        MP1_image_cropped = np.mean(image_data32_cropped, axis=1) #* y_mask
        
        
        bg_MP0 = np.mean(bg_32, axis=0) 
        bg_MP1 = np.mean(bg_32, axis=1)
        
        ratio0 = np.mean(MP0_image_cropped) / np.mean(MP0_infocus_cropped+1e-10) 
        ratio1 = np.mean(MP1_image_cropped) / np.mean(MP1_infocus_cropped+1e-10) 
        
       
    
        Sbg_MP0 = bg_MP0 * ratio0
        Sbg_MP1 = bg_MP1 * ratio1
    
        NMP0 = (MP0_image_cropped - Sbg_MP0) 
        NMP1 = (MP1_image_cropped - Sbg_MP1) 
       
     
        filt_proj = scipy.ndimage.uniform_filter1d(NMP0, size=10, mode='nearest')
        filt_proj1 = scipy.ndimage.uniform_filter1d(NMP1, size=50, mode='nearest')
 

        sp = np.fft.fft(filt_proj)
        sp1 = np.fft.fft(filt_proj1)
        ps = np.abs(sp)**2  
        ps1 = np.abs(sp1)**2  
        freq = np.fft.fftfreq(len(filt_proj)) 
        freq1 = np.fft.fftfreq(len(filt_proj1))
        idx = np.argsort(freq)
        idx1 = np.argsort(freq1)
        spline = UnivariateSpline(np.arange(0,len(ps[idx]),1), (ps[idx]-(ps[idx][int((len(ps[idx]))/2)]/2)), s=0)
        roots = spline.roots()
        if roots.size <2:
            ps_FWHM = 2048
        else:
            upp_root_diff = 3000.0
            low_root_diff = 3000.0
            upper_root = len(freq)/2 + 1
            lower_root = len(freq)/2 - 1
            for r in roots:
                if r >= len(freq)/2:
                    upp_root_d = np.abs(r - len(freq)/2)
                    if upp_root_d < upp_root_diff:
                        upp_root_diff = upp_root_d
                        upper_root = r
                else:
                    low_root_d = np.abs(len(freq)/2 - r)
                    if low_root_d < low_root_diff:
                        low_root_diff = low_root_d
                        lower_root = r
            ps_FWHM = upper_root - lower_root
        spline1 = UnivariateSpline(np.arange(0,len(ps1[idx1]),1), (ps1[idx1]-(ps1[idx1][int((len(ps1[idx1]))/2)]/2)), s=0)
        roots1 = spline1.roots()
        upp_root1_diff = 3000.0
        low_root1_diff = 3000.0
        upper_root1 = len(freq1)/2 + 1
        lower_root1 = len(freq1)/2 - 1
        for r in roots1:
            if r >= len(freq1)/2:
                upp_root1_d = np.abs(r - len(freq1)/2)
                if upp_root1_d < upp_root1_diff:
                    upp_root1_diff = upp_root1_d
                    upper_root1 = r
            else:
                low_root1_d = np.abs(len(freq1)/2 - r)
                if low_root1_d < low_root1_diff:
                    low_root1_diff = low_root1_d
                    lower_root1 = r
        ps_FWHM1 = upper_root1 - lower_root1
  

        image_result.Release()
        self.cam.EndAcquisition()
        print('\r\n')
        return ps_FWHM, ps_FWHM1, image_data, avg_int

    def main(self):
        std_send, std_send2, image_not_send, avg_int = self.calc_std()
       
       # with open(f"S:/2025/AF_Powers_.txt", "w") as f:
        #    for power_frac,FP,NFP,Ratio,avg_int,AvgB_MN,AvgI_MN,AvgI_MB,timestamp in self.FP_NFP_R_L:
         #       f.write(f"{power_frac},{FP},{NFP},{Ratio},{avg_int},{AvgB_MN},{AvgI_MN},{AvgI_MB},{timestamp}\n")
        return std_send, std_send2, avg_int