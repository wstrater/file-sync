package com.wstrater.server.fileSync.common.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Hello {

  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Hello [name=" + name + "]";
  }

}