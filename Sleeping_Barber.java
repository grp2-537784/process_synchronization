
--------------------------------------------------------------------------------------------
-import java.util.concurrent.BlockingQueue;
-import java.util.concurrent.atomic.AtomicBoolean;
-import java.util.concurrent.atomic.AtomicInteger;
-
 public class Barber implements Runnable {
   private static final int HAIRCUT_TIME_MILLIS = 20;
-  private final AtomicBoolean shopOpen;
-  private final BlockingQueue<?> waitingRoom;
-  private final AtomicInteger totalHaircuts;
+  private final BarberShop shop;
 
-  public Barber(AtomicBoolean shopOpen, BlockingQueue<?> waitingRoom, AtomicInteger totalHaircuts) {
-    this.shopOpen = shopOpen;
-    this.waitingRoom = waitingRoom;
-    this.totalHaircuts = totalHaircuts;
+  public Barber(BarberShop shop) {
+    this.shop = shop;
   }
 
   @Override
   public void run() {
-    while(shopOpen.get()) {
+    while(shop.isOpen()) {
       try {
-        waitingRoom.take();
-        Thread.sleep(HAIRCUT_TIME_MILLIS);
-        totalHaircuts.incrementAndGet();
+        Object customer = shop.napUntilCustomerArrives();
+        cutHair(customer);
+        shop.recordHaircut();
       } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         break;
@@ -30,4 +22,8 @@ public void run() {
     }
   }
 
+  private void cutHair(Object customer) throws InterruptedException {
+    Thread.sleep(HAIRCUT_TIME_MILLIS);
+  }
+
 }
-------------------------------------------------------------------------------------------
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.concurrent.TimeUnit.*;

public class BarberShop {
	public static final int NUM_WAITING_ROOM_CHAIRS = 3;
	public static final long SHOP_RUNTIME_MILLIS = SECONDS.toMillis(10);
	private final static AtomicBoolean shopOpen = new AtomicBoolean();
	private final static AtomicInteger totalHaircuts = new AtomicInteger();
	private final static AtomicInteger lostCustomers = new AtomicInteger();
	private final BlockingQueue<Object> waitingRoom = new LinkedBlockingQueue<>(NUM_WAITING_ROOM_CHAIRS);

	public static void main(String[] args) throws InterruptedException {
		BarberShop shop = new BarberShop();

		ExecutorService executor = Executors.newFixedThreadPool(3);

		Runnable customerGenerator = new CustomerGenerator(shop);
		Runnable barber = new Barber(shop);
		Runnable progressTracker = new ProgressTracker(shop);

		shop.open();

		executor.execute(progressTracker);
		executor.execute(barber);
		executor.execute(customerGenerator);
		executor.shutdown();

		Thread.sleep(SHOP_RUNTIME_MILLIS);

		shop.close();
	}

	private void close() {
		shopOpen.set(false);
	}

	private void open() {
		shopOpen.set(true);
	}

	public boolean isOpen() {
		return shopOpen.get();
	}

	public boolean seatCustomerInWaitingRoom(Object customer) {
		boolean customerSeated = waitingRoom.offer(customer);
		if(!customerSeated) {
			lostCustomers.incrementAndGet();
		}
		return customerSeated;
	}

	public Object napUntilCustomerArrives() throws InterruptedException {
		return waitingRoom.take();
	}

	public void recordHaircut() {
		totalHaircuts.incrementAndGet();
	}

	public Object lostCustomers() {
		return lostCustomers.get();
	}

	public Object haircuts() {
		return totalHaircuts.get();
	}

}
---------------------------------------------------------------------------------------------------
 
-import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.ThreadLocalRandom;
-import java.util.concurrent.atomic.AtomicBoolean;
-import java.util.concurrent.atomic.AtomicInteger;
 
 public class CustomerGenerator implements Runnable {
   public static final int ARRIVAL_INTERVAL_OFFSET_MILLIS = 10;
   public static final int ARRIVAL_INTERVAL_RANGE_MILLIS = 20;
-  private final BlockingQueue<Object> waitingRoom;
-  private final AtomicBoolean shopOpen;
-  private final AtomicInteger lostCustomers;
+  private final BarberShop shop;
 
-  public CustomerGenerator(AtomicBoolean shopOpen, BlockingQueue<Object> waitingRoom, AtomicInteger lostCustomers) {
-    this.shopOpen = shopOpen;
-    this.waitingRoom = waitingRoom;
-    this.lostCustomers = lostCustomers;
+  public CustomerGenerator(BarberShop shop) {
+    this.shop = shop;
   }
 
   @Override
   public void run() {
-    while (shopOpen.get()) {
+    while (shop.isOpen()) {
       try {
         Thread.sleep(nextRandomInterval());
-        boolean queued = waitingRoom.offer(new Object());
-        if (!queued) {
-          lostCustomers.incrementAndGet();
-        }
+        shop.seatCustomerInWaitingRoom(new Object());
       } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         break;
@@ -34,7 +24,7 @@ public void run() {
     }
   }
 
-  int nextRandomInterval() {
+  public int nextRandomInterval() {
     return ThreadLocalRandom.current().nextInt(ARRIVAL_INTERVAL_RANGE_MILLIS) + ARRIVAL_INTERVAL_OFFSET_MILLIS;
   }
------------------------------------------------------------------------------------------------------------------------
 
-import java.util.concurrent.atomic.AtomicBoolean;
-import java.util.concurrent.atomic.AtomicInteger;
-
 public class ProgressTracker implements Runnable {
 
-  private final AtomicInteger lostCustomers;
-  private final AtomicInteger totalHaircuts;
-  private final AtomicBoolean shopOpen;
+  private final BarberShop shop;
 
-  public ProgressTracker(AtomicBoolean shopOpen, AtomicInteger totalHaircuts, AtomicInteger lostCustomers) {
-    this.shopOpen = shopOpen;
-    this.totalHaircuts = totalHaircuts;
-    this.lostCustomers = lostCustomers;
+  public ProgressTracker(BarberShop shop) {
+    this.shop = shop;
   }
 
   @Override
   public void run() {
-    while (shopOpen.get()) {
+    while (shop.isOpen()) {
       try {
         Thread.sleep(100);
         printProgress();
@@ -27,10 +20,10 @@ public void run() {
       }
     }
     printProgress();
-    System.out.println("");
+    System.out.println();
   }
 
   private void printProgress() {
-    System.out.printf("The shop served %s customers but turned away %s.\r", totalHaircuts, lostCustomers);
+    System.out.printf("The shop served %s customers but turned away %s.\r", shop.haircuts(), shop.lostCustomers());
   }
 }
