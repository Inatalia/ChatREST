package com.restfully.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author  Irene Hardjono
 * @version $Revision: 1 $
 */
@Path("chat")
public class ChatServices
{
   class Message
   {
      String id;
      String message;
      byte[] file;
      Message next;
   }

   protected Message first;
   protected Message last;
   protected int maxMessages = 10;
   protected LinkedHashMap<String, Message> messages = new LinkedHashMap<String, Message>()
   {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Message> eldest)
      {
         boolean remove = size() > maxMessages;
         if (remove) first = eldest.getValue().next;
         return remove;
      }
   };

   protected AtomicLong counter = new AtomicLong(0);

   LinkedList<AsyncResponse> listeners = new LinkedList<AsyncResponse>();

   ExecutorService writer = Executors.newSingleThreadExecutor();

   @Context
   protected UriInfo uriInfo;

   @POST
   @Consumes("text/plain")
   public void post(final String text)
   {
      final UriBuilder base = uriInfo.getBaseUriBuilder();
      writer.submit(new Runnable()
      {
         @Override
         public void run()
         {
            synchronized (messages)
            {
               Message message = new Message();
               message.id = Long.toString(counter.incrementAndGet());
               message.message = text;

               if (messages.size() == 0)
               {
                  first = message;
               }
               else
               {
                  last.next = message;
               }
               messages.put(message.id, message);
               last = message;

               for (AsyncResponse async : listeners)
               {
                  try
                  {
                     send(base, async, message);
                  }
                  catch (Exception e)
                  {
                     e.printStackTrace();
                  }
               }
               listeners.clear();
            }
         }
      });
   }
   
   @POST
   @Produces("multipart/form-data")
   @Consumes("multipart/form-data")
   public MultipartFormDataOutput uploadFile( MultipartFormDataInput input) throws IOException
   {
	   Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
       //Get file data to save
       List<InputPart> inputParts = uploadForm.get("file1");
       for (InputPart inputPart : inputParts)
       {
           try
           {
               //header for extra processing if required
               //MultivaluedMap<String, String> header = inputPart.getHeaders();
               //convert the uploaded file to inputstream and write it to disk
               InputStream inputStream = inputPart.getBody(InputStream.class, null);
               File file = new File("/Users/irenenatalia/Desktop/output/output.jpg");
               OutputStream out = new FileOutputStream(file);
               int read = 0;
               byte[] bytes = new byte[2048];
               while ((read = inputStream.read(bytes)) != -1) {
                  out.write(bytes, 0, read);
               }
               inputStream.close();
               out.flush();
               out.close();
               System.out.println(file.getName() + " has been UPLOADED in " + file.getAbsolutePath());
           }
           catch (Exception e)
           {
               e.printStackTrace();
           }
       }
       
        // return the result
       MultipartFormDataOutput mdo = new MultipartFormDataOutput();
       mdo.addFormData("file2", new FileInputStream(new File("/Users/irenenatalia/Desktop/output/output.jpg")), MediaType.APPLICATION_OCTET_STREAM_TYPE);
       //return mdo;
       return null;
   }
   
   @GET
   public void receive(@QueryParam("current") String next, @Suspended AsyncResponse async)
   {
      final UriBuilder base = uriInfo.getBaseUriBuilder();
      Message message = null;
      synchronized (messages)
      {
         Message current = messages.get(next);
         if (current == null) message = first;
         else message = current.next;

         if (message == null) {
            queue(async);
         }
      }
      // do this outside of synchronized block to reduce lock hold time
      if (message != null) send(base, async, message);
    }

   protected void queue(AsyncResponse async)
   {
      listeners.add(async);
   }

   protected void send(UriBuilder base, AsyncResponse async, Message message)
   {
      URI nextUri = base.clone().path(ChatServices.class)
              .queryParam("current", message.id).build();
      Link next = Link.fromUri(nextUri).rel("next").build();
      Response response = Response.ok(message.message, MediaType.TEXT_PLAIN_TYPE).links(next).build();
      async.resume(response);
   }
}
