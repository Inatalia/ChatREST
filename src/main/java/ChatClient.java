import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.plugins.providers.multipart.OutputPart;

import javax.ws.rs.FormParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Irene Hardjono
 * @version $Revision: 1 $
 */
public class ChatClient
{
   public static void main(String[] args) throws Exception
   {
	  System.out.println("Welcome to Chat Application!");
	  Scanner sc = new Scanner(System.in);
      String name = "";//args[0];
      while(name.isEmpty()){
    	  System.out.print("Please enter username: ");
    	  name = sc.nextLine();
    	  if(name.equals("\n")){
    		  name = "";
    	  } else {
    		  System.out.println("Username " + name + " has been sucessfully created!");
    		  break;
    	  }
      }
      final String username = name;

      System.out.println();
      System.out.print(username + " > ");
      
      final Client client = new ResteasyClientBuilder()
                          .connectionPoolSize(3)
                          .build();
      WebTarget target = client.target("http://localhost:8080/services/chat");

      target.request().async().get(new InvocationCallback<Response>()
      {
         @Override
         public void completed(Response response)
         {
            Link next = response.getLink("next");
            String message = response.readEntity(String.class);
            //System.out.println();
            System.out.print(message);// + "\r");
            System.out.println();
            System.out.print(username + " > ");
            client.target(next).request().async().get(this);
         }

         @Override
         public void failed(Throwable throwable)
         {
            System.err.println("FAILURE!");
         }
      });


      while (true)
      {
         //System.out.print(username + " > ");
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         String message = br.readLine();
         if(message.toLowerCase().contains("/upload")){
        	 String filename = message.split(" ")[1];
        	 File file = new File(filename);
        	 if(filename.isEmpty()){
        		 target.request().post(Entity.text(name + ": No filename specified!"));
        	 }
        	 else if(!file.exists()){
        		 target.request().post(Entity.text(name + ": Invalid filename!"));
        	 }
        	 else if(file.isFile()){
        		 MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        	       	mdo.addFormData("file1", new FileInputStream(file), 
        	    		   MediaType.APPLICATION_OCTET_STREAM_TYPE);
        	     GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {};
        	     target.request().post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
                 target.request().post(Entity.text("(" + name + ": " + file.getName() + ")"));
        	 }
         }
         else if(message.toLowerCase().equals("/exit")) {
        	 //target.request().get().close();
        	 client.close();
        	 break;
         }
         else{
     	 	target.request().post(Entity.text("(" + name + ": " + message + ")"));
      	 }
      }
   }
}
