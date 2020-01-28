# Primitive Actions

This lists all primitive actions included in Terry's initial corpus. `action.addState(State(name,value,args,transition))` defines what state transitions and associated driver executions occur when this action is called.

<hr>

## mouseToXY

### Description

Moves the mouse to screen coordinates (`x`,`y`), where both are integer numbers. Later on I want to modify this to also have a time argument for controlling easing speed.

### Pattern

`?move) |mouse,cursor,pointer,) to ?|location,position,coordinates,)) ?ex) @#x |ex,comma,why) @#y ?why)`

### States

```java
new State<Point2D>("mouseat", new Point2D.Float(0,0), new String[] {"x","y"}, new DriverExecution<Point2D>() {
    public Point2D execute(Point2D stateOld, Arg[] args) {
        Float x = (Float) args[0].value;
        Float y = (Float) args[1].value;
        Driver.point(x,y);
        return new Point2D.Float(x,y);
    }
});
```

<hr>

## typeStr

### Description

Controls the keyboard to type a string `str`. Later on I want this to have a time argument for controlling typing speed.

### Pattern

`type ?out) ?following string) @$str ?end quote)`

### States

```java
new State<String>("typed", "", new String[] {"str"}, new DriverExecution<String>() {
    public String execute(String stateOld, Arg[] args) {
        String string = (String) args[0].value;
        Driver.type(string);
        return string;
    }
});
```

<hr>

## mouseClickBtn

### Description

Clicks a mouse button (left or right) specified by the `btn` argument.

### Pattern

`?|left,right,)) click`

### States

```java
new State<Integer>("clickbtn", new Integer(), new String[] {"btn"}, new DriverExecution<Integer>() {
    public Integer execute(Integer stateOld, Arg[] args) {
        Integer button = (Integer) args[0].value;
        if (button == MouseEvent.BUTTON1) {
            Driver.clickLeft();
        }
        else if (button == MouseEvent.BUTTON2) {
            Driver.clickRight();
        }
        return button;
    }
});
```

## shutdown

### Description

Shuts down (quits) Terry directly by invoking the intercom window's close event.

### Pattern

`|[shut_down],[turn_off],quit,)`

### States

```java
new State<Boolean>("quitted", false, new String[] {}, new DriverExecution<Boolean>() {
	public Boolean execute(Boolean stateOld, Arg[] args) {
		//no args
		//direct prompter
		try {
			prompter.stop();
		} 
		catch (Exception e) {
			Logger.logError("shutdown failed");
			Logger.logError(e.getMessage());
		}
		return true;
	}
});
```

## driverDemo1

### Description

Executes a hardcoded set of driver pointer and typer commands, showing full mouse control and keyboard support with alphanumeric keys, key-combos, and control keys.

### Pattern

`?do) ?driver) |demo,demonstration,) |one,won,run,)`

### States

```java
new State<Integer>("demoed", 0, new String[] {}, new DriverExecution<Integer>() {
	public Integer execute(Integer stateOld, Arg[] args) {
		//no args
		//direct driver
		new DriverThread() {
			public void run() {
				Logger.log("typing in spotlight...");
				Driver.point(930, 30); //go to eclipse
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				Driver.clickLeft(); //click window
				try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for refocus
				Driver.point(1375, 12); //go to spotlight
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				Driver.clickLeft(); //click icon
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				Driver.type("this is a hello torry#lft)#lft)#lft)#bck)e#lft)#lft)from ");
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				Driver.type("#cmd+rgt)#exl)"); //shift to end and add !
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				Driver.type("#cmd+bck)"); //clear search
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				Driver.type("#lpr)#amp) I can use punctuation too#rpr)#tld)"); //show off punctuation
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				Driver.type("#cmd+bck)#esc)"); //clear search and exit
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				Logger.log("quitting via mouse...");
				Driver.point(755, 899); //go to dock
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				Driver.point(908, 860); //go to java
				Driver.clickRight(); //right-click menu
				try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for os to show options
				Driver.point(934, 771); //close option
			}
		}.start();
		//update state
		return 1;
	}
});
```