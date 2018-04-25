package com.example.steveq.remotehomeclient.services;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionChecker {
    private static final String TAG = PermissionChecker.class.getSimpleName();

    private Activity activity;

    public PermissionChecker(Activity contextActivity){
        activity = contextActivity;
    }

    public List<String> getFalsyPermissions(String[] permissions){
        return permissionNotGranted(permissions);
    }

    public void requestPermissions(List<String> falsyPermissions, int requestId){
        ActivityCompat.requestPermissions(
                activity,
                falsyPermissions.toArray(new String[0]),
                requestId
        );
    }

    public boolean handlePermission(String[] permissions, int requestId){
        List<String> falsyPermissions = permissionNotGranted(permissions);

        if(falsyPermissions.size() > 0){
            return false;
        } else {
            return true;
        }
    }

    public List<String> permissionNotGranted(String[] permissions){

        return Arrays.stream(permissions)
                .filter(p -> {
                    return ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED;
                })
                .collect(Collectors.toList());
    }

    private class PermissionStatus{
        private String permission;
        private Boolean status;

        public PermissionStatus(){}

        public PermissionStatus(String permission, Boolean status) {
            this.permission = permission;
            this.status = status;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public Boolean getStatus() {
            return status;
        }

        public void setStatus(Boolean status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "PermissionStatus{" +
                    "permission='" + permission + '\'' +
                    ", status=" + status +
                    '}';
        }
    }
}
