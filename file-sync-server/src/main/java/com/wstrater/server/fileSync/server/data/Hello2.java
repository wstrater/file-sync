package com.wstrater.server.fileSync.server.data;

public class Hello2 {

  private String name;

  public synchronized String getName() {
    return name;
  }

  public synchronized void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Hello2 [name=" + name + "]";
  }
  
}
