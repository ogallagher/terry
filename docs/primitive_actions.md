# Primitive Actions

This lists all primitive actions included in Terry's initial corpus. `action.addState(State(name,value,args,transition))` defines what state transitions and associated driver executions occur when this action is called.

<hr>

## mouseToXY

### Description

Moves the mouse to screen coordinates (`x`,`y`), where both are integer numbers. Later on I want to modify this to also have a time argument for controlling easing speed.

### Pattern

`?|move,go,)) ?|mouse,cursor,pointer,)) to ?|location,position,coordinates,)) ?x) @#x |x,comma,y,) @#y ?y)`

### States

```java
new State<Point2D>("mouseat", new Point2D.Float(0,0), new String[] {"x","y"}, new DriverExecution<Point2D>() {
	public Point2D execute(Point2D stateOld, Arg[] args) {
		Float x = Float.valueOf(0);
		Float y = Float.valueOf(0);

		//map args
		for (Arg arg : args) {
			Object value = arg.getValue();

			if (arg.name.equals("x")) {
				x = (Float) value;
			}
			else if (arg.name.equals("y")) {
				y = (Float) value;
			}
		}

		//direct driver
		Driver.point(x.intValue(), y.intValue());

		//update state
		return new Point2D.Float(x, y);
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
		String string = "";
				
		//map args
		for (Arg arg : args) {
			if (arg.name.equals("str")) {
				string = (String) arg.getValue();
			}
		}
		
		//direct driver
		Driver.type(string);
		
		//update state
		return string;
    }
});
```

<hr>

## mouseClickBtn

### Description

Clicks a mouse button (left or right) specified by the `btn` argument, of type direction.

### Pattern

`?@dbtn) click`

### States

```java
new State<Integer>("clickbtn", new Integer(), new String[] {"btn"}, new DriverExecution<Integer>() {
    public Integer execute(Integer stateOld, Arg[] args) {
        MouseButton button = MouseButton.PRIMARY;
        
		//map args
		for (Arg arg : args) {
			Object value = arg.getValue();

			if (value == null) {
				Logger.log("null arg");
			}
			else if (arg.name.equals("btn")) {
				String direction = (String) value;

				if (direction.equals(Arg.DIRARG_RIGHT)) {
					button = MouseButton.SECONDARY;
				}
				else if (direction.equals(Arg.DIRARG_MIDDLE)) {
					button = MouseButton.MIDDLE;
				}
				//else, assume button 1
			}
		}

		//direct driver
		if (button == MouseButton.PRIMARY) {
			Driver.clickLeft();
		}
		else if (button == MouseButton.SECONDARY) {
			Driver.clickRight();
		}
		else if (button == MouseButton.MIDDLE) {
			Driver.clickMiddle();
		}

		//update state
		return button;
    }
});
```

<hr>

## mouseDragXY

### Description

Drags the mouse to screen coordinates (`x`,`y`), both being integers.

### Pattern

`?drag) ?|mouse,cursor,pointer,)) to ?|location,position,coordinates,)) ?x) @#x |x,comma,y,) @#y ?y)`

### States

```java
new State<>("mousedragged", new Point2D.Float(), new String[] {"x","y"}, new Execution<Point2D>() {
	public Point2D execute(Point2D stateOld, Arg[] args) {
		Float x = Float.valueOf(0);
		Float y = Float.valueOf(0);
		
		//map args
		for (Arg arg : args) {
			Object value = arg.getValue();
			
			if (arg.name.equals("x")) {
				x = (Float) value;
			}
			else if (arg.name.equals("y")) {
				y = (Float) value;
			}
		}
		
		//direct driver
		Driver.drag(x.intValue(), y.intValue());
		
		//update state(s)
		Point2D.Float dest = new Point2D.Float(x, y);
		mouseat.getProperty().set(dest);
		return dest;
	}
});
```

<hr>

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

<hr>

## showState

### Description

Prints out all actions' states' current values from the state table `Terry.states`.

### Pattern

`|show,[what_is],log,) ?current) state`

### States

```java
new State<Boolean>("stateshown", true, new String[] {}, new Execution<Boolean>() {
	public Boolean execute(Boolean stateOld, Arg[] args) {
		//no args
		//direct controller
		Logger.log("states: ");
		for (String state : Terry.printState()) {
			Logger.log(state);
		}
		
		return true;
	}
});
```

<hr>

## captureScreen

### Description

Takes a full-screen screen capture. The `statecaptureupdated` state is a convenience for other actions to know when `statecaptured` is ready to have its image read.

### Pattern

`?|create,take,)) |screenshot,[screen_shot],[screen_capture],[capture_screen],)`

### States

```java
new State<Boolean>("statecaptureupdated", Boolean.FALSE, new String[] {}, new Execution<Boolean>() {
	public Boolean execute(Boolean stateOld, Arg[] args) {
		return false;
	}
});

new State<BufferedImage>("statecaptured", new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), new String[] {}, new Execution<BufferedImage>() {
	public BufferedImage execute(BufferedImage stateOld, Arg[] args) {
		//no args
		//direct driver and prompter
		Prompter.hide();
		
		Dimension screen = Driver.getScreen();
		BufferedImage capture = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_RGB);
		
		//capture screen
		Driver.captured.addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					SwingFXUtils.fromFXImage(Driver.capture, capture);
					
					//update statecaptureupdated when the capture object contains the data
					SimpleObjectProperty<Boolean> captureUpdated = statecaptureupdated.getProperty();
					captureUpdated.set(true);
					
					if (capture != null) {
						Utilities.saveImage(capture, Terry.RES_PATH + "vision/", "screen_capture.png");
					}
					
					Prompter.show();
					
					Driver.captured.removeListener(this);
				}
			}
		});
		Driver.captureScreen();
		
		return capture;
	}
});
```

<hr>

## locateWidget

### Description

Takes a screen capture and then searches for the widget specified by the `widget` argument in the image. Details as to whether to use text recognition or image recognition are left to the widget to determine. `widgetlocationupdated` notifies that the search result is ready, and `widgetlocation` stores the result.

### Pattern

`?|find,locate,show,) ?where) ?is) @wwidget ?is)`

### States

```java
new State<Boolean>("widgetlocationupdated", Boolean.FALSE, new String[] {}, new Execution<Boolean>() {
	public Boolean execute(Boolean stateOld, Arg[] args) {
		return false;
	}
});

new State<Point2D>("widgetlocation", new Point2D.Double(), new String[] {"widget"}, new Execution<Point2D>() {
	public Point2D execute(Point2D stateOld, Arg[] args) {
		//map args
		Widget widget = null;
		
		for (Arg arg : args) {	
			if (arg != null && arg.name.equals("widget")) {
				widget = (Widget) arg.getValue();
			}
		}
		
		Point2D location = new Point2D.Double(-1,-1);
		
		//direct various modules
		if (widget != null) {										
			//direct driver to capture screen
			Logger.log("preparing to locate widget " + widget.getName());
			final Widget finalWidget = widget;
			
			HashMap<String,Arg> captureScreenArgs = new HashMap<>();
			try {
				//listen for when screen capture is complete
				SimpleObjectProperty<Boolean> captureUpdated = statecaptureupdated.getProperty();
				captureUpdated.set(false);
				captureUpdated.addListener(new ChangeListener<Boolean>() {
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
						if (newValue.booleanValue()) {
							//get capture and search
							BufferedImage screenshot;
							try {
								screenshot = statecaptured.getValue();
								
								//handle result of widget search
								finalWidget.state.set(Widget.STATE_IDLE);
								finalWidget.state.addListener(new ChangeListener<Character>() {
									public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
										boolean removeme = false;
										
										if (newValue == Widget.STATE_FOUND) {
											Rectangle zone = finalWidget.getZone();
											
											//direct prompter to highlight found widget
											Prompter.clearOverlay();
											Prompter.showOverlay();
											Prompter.colorOverlay(new Color(0.8,0.1,0.6,0.2), Color.MEDIUMVIOLETRED);
											Prompter.drawOverlay(zone.getPathIterator(null), true, true);
											
											//update state(s)
											location.setLocation(zone.getCenterX(), zone.getCenterY());
											widgetlocationupdated.getProperty().set(true);
											removeme = true;
										}
										else if (newValue == Widget.STATE_NOT_FOUND) {
											Logger.log("widget not found");
											removeme = true;
										}
										
										if (removeme) {
											finalWidget.state.removeListener(this);
										}
									}
								});
								
								//direct widget to find itself
								finalWidget.findInScreen(screenshot);
							}
							catch (WidgetException e) {
								Logger.logError("widget search failed: " + e.getMessage());
							}
							
							captureUpdated.removeListener(this);
						}
					}
				});
				
				//execute screen capture action
				captureScreen.execute(captureScreenArgs);
			} 
			catch (StateException e) {
				Logger.logError(e.getMessage());
			}
		}
		else {
			Logger.logError("widget to find was not given");
		}
		
		return location;
	}
});
```

<hr>

## mouseToWidget

### Description

Moves the mouse to a widget specified by the `widget` argument. Calls subaction `locateWidget` to determine the destination for the cursor movement.

### Pattern

`|[?move)_|mouse,cursor,pointer,)],go,) to @wwidget`

### States

```java
new State<Widget>("mouseatwidget", dummyWidget, new String[] {"widget"}, new Execution<Widget>() {
	public Widget execute(Widget stateOld, Arg[] args) {
		//map args
		Widget widget = null;
		Arg widgetArg = null;
		
		for (Arg arg : args) {	
			if (arg != null && arg.name.equals("widget")) {
				widget = (Widget) arg.getValue();
				widgetArg = arg;
			}
		}
		
		if (widget != null) {
			//get widget location
			HashMap<String,Arg> locateWidgetArgs = new HashMap<>();
			locateWidgetArgs.put("widget", widgetArg);
			
			try {
				SimpleObjectProperty<Boolean> widgetLocationUpdated = widgetlocationupdated.getProperty();
				widgetLocationUpdated.set(false);
				widgetLocationUpdated.addListener(new ChangeListener<Boolean>() {
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {								
						if (newValue) {
							//move mouse to widget location
							Point2D location = widgetlocation.getProperty().get();
							int x = (int) location.getX();
							int y = (int) location.getY();
			
							//direct driver
							Driver.point(x, y, notifier);
			
							//update mouse location
							mouseat.getProperty().set(new Point2D.Float(x,y));
			
							widgetLocationUpdated.removeListener(this);
						}
					}
				});
				
				locateWidget.execute(locateWidgetArgs);
				
				//update moused widget
				return widget;
			}
			catch (StateException e) {
				//widget location failure
				return null;
			}					
		}
		else {
			return null;
		}
	}
});
```

<hr>

## mouseDragWidget

### Description

Like `mouseToWidget`, but the mouse is dragged instead of moved. The `dummyWidget` argument refers to `Terry.dummyWidget`, which has a few purposes throughout the program. Generally, it's a placeholder for a widget referring to nothing, without being equal to null.

### Pattern

`drag ?|mouse,cursor,pointer,)) to @wwidget`

### States

```java
new State<Widget>("mousedraggedwidget", dummyWidget, new String[] {"widget"}, new Execution<Widget>() {
	public Widget execute(Widget stateOld, Arg[] args) {
		//map args
		Widget widget = null;
		Arg widgetArg = null;
	
		for (Arg arg : args) {	
			if (arg != null && arg.name.equals("widget")) {
				widget = (Widget) arg.getValue();
				widgetArg = arg;
			}
		}
	
		if (widget != null) {
			//get widget location
			HashMap<String,Arg> locateWidgetArgs = new HashMap<>();
			locateWidgetArgs.put("widget", widgetArg);
		
			try {
				SimpleObjectProperty<Boolean> widgetLocationUpdated = widgetlocationupdated.getProperty();
				widgetLocationUpdated.set(false);
				widgetLocationUpdated.addListener(new ChangeListener<Boolean>() {
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {								
						if (newValue) {
							//move mouse to widget location
							Point2D location = widgetlocation.getProperty().get();
							int x = (int) location.getX();
							int y = (int) location.getY();
						
							//direct driver
							Driver.drag(x, y, notifier);
						
							//update mouse location
							Point2D.Float dest = new Point2D.Float(x,y);
							mouseat.getProperty().set(dest);
							mousedragged.getProperty().set(dest);
						
							widgetLocationUpdated.removeListener(this);
						}
					}
				});
			
				locateWidget.execute(locateWidgetArgs);
			
				//update moused widget
				mouseatwidget.getProperty().set(widget);
				return widget;
			}
			catch (StateException e) {
				//widget location failure
				return null;
			}					
		}
		else {
			return null;
		}
	}
});
```

<hr>

## driverDemo1

### Description

Executes a hardcoded set of driver pointer and typer commands, showing full mouse control and keyboard support with alphanumeric keys, key-combos, and control keys.

### Pattern

`?do) ?driver) |demo,demonstration,) |one,won,run,)`

### States

```java
new State<Integer>("driverdemoed", 0, new String[] {}, new Execution<Integer>() {
	public Integer execute(Integer stateOld, Arg[] args) {
		//no args
		//direct driver
		Logger.log("typing in spotlight...");
		Driver.point(930, 30, null); //go to eclipse
		
		try {Thread.sleep(500);} catch (InterruptedException e) {}
		
		Driver.clickLeft(null); //click window
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for refocus
		
		Driver.point(1375, 12, null); //go to spotlight
		
		try {Thread.sleep(500);} catch (InterruptedException e) {}
		
		Driver.clickLeft(null); //click icon
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {}
		
		Driver.type("this is a hello torry#lft)#lft)#lft)#bck)e#lft)#lft)from ", null);
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {}
		
		Driver.type("#cmd+rgt)#exl)", null); //shift to end and add !
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {}
		
		Driver.type("#cmd+bck)", null); //clear search
		
		try {Thread.sleep(500);} catch (InterruptedException e) {}
		
		Driver.type("#lpr)#amp) I can use punctuation too#rpr)#tld)", null); //show off punctuation
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {}
		
		Driver.type("#cmd+bck)#esc)", null); //clear search and exit
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {}
		
		Logger.log("quitting via mouse...");
		Driver.point(755, 899, null); //go to dock
		
		try {Thread.sleep(500);} catch (InterruptedException e) {}
		
		Driver.point(908, 860, null); //go to java
		
		Driver.clickRight(null); //right-click menu
		
		try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for os to show options
		
		Driver.point(934, 771, notifier); //close option. last action; attach notifier
		
		//update state
		return 1;
	}
});
```

<hr>

## setSpeakerVolume

### Description

Exposes Speaker.setVolume() to the user. Accepts a range from 1-10 by default, but supports percentages if "percent" is said explicitly, or 0-1 range if the value is less than 1.

### Pattern

`?set) ?|speaker,speech,spoken)) volume to @#level ?@$percent)`

### States

```java
new State<Float>("speakervolume", 0.5f, new String[] {"level","percent"}, new Execution<Float>() {	
	public Float execute(Float stateOld, Arg[] args) {
		//map args
		Float volume = null;
		String percent = null;
		
		for (Arg arg : args) {
			if (arg != null) {
				if (arg.name.equals("level")) {
					volume = (Float) arg.getValue();
				}
				else if (arg.name.equals("percent")) {
					percent = (String) arg.getValue();
				}
			}
		}
		
		//control speaker
		if (volume != null && volume > 0) {
			if (percent != null && percent.equals("percent")) { //percent was said explicitly, 0-100 range
				volume = volume / 100;
			}
			else if (volume >= 1 && volume <= 10) { //assume 0-10 range
				volume = volume / 10;
			}
			//else assume 0-1 range
			
			try {
				Speaker.setVolume(volume);
				Logger.log("now i speak this loud", Logger.LEVEL_SPEECH);
				return volume;
			}
			catch (SpeakerException e) {
				Logger.logError(e.getMessage(), Logger.LEVEL_SPEECH);
				return null;
			}
		}
		else {
			//no volume specified
			Logger.logError("volume level " + volume + " not specified or invalid", Logger.LEVEL_CONSOLE);
			return null;
		}
	}
});
```

## setSpeakerSpeed

### Description

Exposes Speaker.setSpeed() to the user. This action uses the same value ranges as setSpeakerVolume.

### Pattern

`?set) ?|speaker,speech,spoken,)) speed to @#speed ?@$percent)`

### States

new State<Float>("speakerspeed", 0.5f, new String[] {"speed","percent"}, new Execution<Float>() {
	public Float execute(Float stateOld, Arg[] args) {
		//map args
		Float speed = null;
		String percent = null;
		
		for (Arg arg : args) {
			if (arg != null) {
				if (arg.name.equals("speed")) {
					speed = (Float) arg.getValue();
				}
				else if (arg.name.equals("percent")) {
					percent = (String) arg.getValue();
				}
			}
		}
		
		//control speaker
		if (speed != null && speed > 0) {
			if (percent != null && percent.equals("percent")) { //percent was said explicitly, 0-100 range
				speed = speed / 100;
			}
			else if (speed >= 1 && speed <= 10) { //assume 0-10 range
				speed = speed / 10;
			}
			//else assume 0-1 range
			
			try {
				Speaker.setSpeed(speed);
				Logger.log("now i speak this fast", Logger.LEVEL_SPEECH);
				return speed;
			}
			catch (SpeakerException e) {
				Logger.logError(e.getMessage(), Logger.LEVEL_SPEECH);
				return null;
			}
		}
		else {
			//no volume specified
			Logger.logError("speech speed " + speed + " not specified or invalid", Logger.LEVEL_CONSOLE);
			return null;
		}
	}
});

## setSpeakerVoice

### Description

Exposes Speaker.setVoice() to the user. If the specified voice name corresponds to a valid voice on the machine, the speaker's voice is changed accordingly.

### Pattern

`?set) ?|speaker,speech,spoken,)) voice to @$voice`

### States

new State<String>("speakervoice", "", new String[] {"voice"}, new Execution<String>() {
	public String execute(String stateOld, Arg[] args) {
		//map args
		String voice = null;
		
		for (Arg arg : args) {
			if (arg.name.equals("voice")) {
				voice = (String) arg.getValue();
			}
		}
		
		//control speaker
		if (voice != null) {
			try {
				Speaker.setVoice(voice);
				Logger.log("speaker voice changed to " + voice, Logger.LEVEL_SPEECH);
				return voice;
			} 
			catch (SpeakerException e) {
				Logger.logError(e.getMessage(), Logger.LEVEL_SPEECH);
				return null;
			}
		}
		else {
			Logger.logError("no voice was specified", Logger.LEVEL_SPEECH);
			return null;
		}
	}
	
});

## overlayDemo1

### Description

Demonstrates the `Prompter.overlay` window by animating a colored circle over it.

### Pattern

`overlay |demo,demonstration,) |one,1,)`

### States

```java
new State<Integer>("overlaydemoed", 0, new String[] {}, new Execution<Integer>() {	
	public Integer execute(Integer stateOld, Arg[] args) {
		//no args
		//direct prompter
		Prompter.showOverlay(null);
		Prompter.colorOverlay(Color.MEDIUMPURPLE, Color.PURPLE);
		
		Dimension screen = Driver.getScreen();
		Ellipse2D ball = new Ellipse2D.Double(20,20,20,20);
		AffineTransform t = new AffineTransform();
		int vx = 1; int vy = 1;
		
		SimpleObjectProperty<Integer> overlaydemoed = (SimpleObjectProperty<Integer>) states.get("overlaydemoed").getProperty();
		overlaydemoed.set(1);
		
		boolean go = true;
		Logger.log("drawing overlay circle");
		while (go && overlaydemoed.get().equals(1)) {
			t.translate(vx, vy);
			
			if (t.getTranslateX() > screen.width || t.getTranslateX() < 0) {
				vx = -vx;
				t.translate(vx, 0);
			}
			if (t.getTranslateY() > screen.height || t.getTranslateY() < 0) {
				vy = -vy;
				t.translate(0, vy);
			}
			
			Prompter.clearOverlay(null);
			Prompter.drawOverlay(ball.getPathIterator(t), true, true, null);
			
			try {
				Thread.sleep(10);
			} 
			catch (InterruptedException e) {
				go = false;
			}
		}
		Logger.log("drawing done");
		Prompter.hideOverlay(null);
		
		//update state
		notifier.set(true);
		return 1;
	}
});
```