package com.restfully.shop.services;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/services")
public class ChatApplication extends Application
{
   private Set<Object> singletons = new HashSet<Object>();

   public ChatApplication()
   {
      singletons.add(new ChatServices());
   }

   @Override
   public Set<Object> getSingletons()
   {
      return singletons;
   }
}
