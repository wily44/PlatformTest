import org.newdawn.slick.*;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.state.*;
import org.newdawn.slick.tiled.*;

public class GameplayState extends BasicGameState {

	//*****NOTE: change all "30" to variable = tile width
	
	PlatformLevel lvl1 = null;
	Character player1 = null;
	Camera cam = null;
	int camLine;
	
	//TiledMap map;
	
	int stateID = -1;
	 
    GameplayState( int stateID ) 
    {
       this.stateID = stateID;
    }
 
    @Override
    public int getID() {
        return stateID;
    }
 
    public void init(GameContainer gc, StateBasedGame sbg) throws SlickException
    {
    	//map = new TiledMap("data/TestTileMap.tmx","data");
    	cam = new Camera();
    	camLine = 400; //where camera begins to move
    	
		lvl1 =  new PlatformLevel(1); //create new level object
		lvl1.loadBackground("data/testbackground.png");
		lvl1.loadSound("data/lvl1track.wav");
		lvl1.loadTiles("data/testfloormap2.tmx");
		lvl1.loadNPC();
		
		player1 = new Character();
		player1.loadAnimation("data/testplayer", ".png", 3);
		player1.x = 0;
		player1.y = 400 - player1.height;
    }
 
    public void update(GameContainer gc, StateBasedGame sbg, int delta) throws SlickException
    {
    	Input input = gc.getInput();
    	
    	gc.setMaximumLogicUpdateInterval(17); //consistent logic rate
		
    	//level sound:
    	lvl1.playMusic();
    	
    	SoundStore.get().poll(0); //may not be needed
    	
		if (player1.isAlive()) //only move player if still alive, otherwise death animation
		{
			wall(player1, lvl1); //keeps from moving off left of screen
			
			if (input.isKeyDown(Input.KEY_D)) //normal movement on ground
			{
				//add horizontal acceleration in air
				player1.x += player1.xSpeed * delta;
				player1.checkCollision(lvl1, gc, "right"); //check for each input
				
				player1.animateRight();
			}
			if (input.isKeyDown(Input.KEY_A)) {
				player1.x -= player1.xSpeed * delta;
				player1.checkCollision(lvl1, gc, "left");
				
				player1.animateLeft();
			}
			if (input.isKeyDown(Input.KEY_W)) //jump command
			{
				player1.jump(delta);
			}
			//check attack key after movement update
			if (input.isKeyDown(Input.KEY_SPACE)) //normal movement on ground
			{
				player1.updateWeapon(); //temporary attack test
				player1.attack();
			}
			else
			{
				player1.isAttacking = false;
				player1.attackCounter = 0; //reset counter
			}
		}
		if(input.isKeyDown(Input.KEY_P)) //pause menu
		{
			sbg.enterState(TestPlatformer.PAUSESTATE);
		}
		
		lvl1.updateNPC(player1);
		//will be arrays of all objects
		
		if(player1.isAlive()) //only move player if still alive
		{
			gravity(player1, gc);
			
			for(int i = 0; i < lvl1.allNPC.size(); i++)
			{
				if(player1.checkHurtBox(lvl1.allNPC.get(i)))
				{
					player1.die();
				}
			}
		}
		
		//Temporary movement bypass cheat:
		if (input.isKeyDown(Input.KEY_LSHIFT)) //jump command
		{
			player1.y += player1.gCount;
			player1.ySpeed = 0;
			player1.gCount = 0;
			
			if(input.isKeyDown(Input.KEY_D))
			{
				player1.x += player1.xSpeed * delta; //doubles speed
			}
			if(input.isKeyDown(Input.KEY_A))
			{
				player1.x -= player1.xSpeed * delta;
			}
			if(input.isKeyDown(Input.KEY_S))
			{
				player1.y += player1.xSpeed * delta;
			}
		}
		
		//gravity(lvl1.testNPC, gc);
		//world.wall(lvl1.testNPC);
		
		for(int i = 0; i < lvl1.allNPC.size(); i++)
		{
			gravity(lvl1.allNPC.get(i), gc);
			//wall(lvl1.allNPC.get(1));
		}
		
		cam.xCamShift(lvl1, player1, camLine);
    }
	
    public void render(GameContainer gc, StateBasedGame sbg, Graphics g) throws SlickException
    {
    	
    	lvl1.drawLevel(cam.xShift); //draws Background
    	//map.render(0, 0, 0, 0, 800, 600); //test render of tilemap
    	player1.draw(cam.xShift);
    	
    	g.drawString("Life: " + player1.getLife(), 725, 10); //life counter in upper right
    
    	Input input = gc.getInput();
    	if(input.isKeyDown(Input.KEY_LSHIFT))
    	{
    		g.drawString("whosyourdaddy", 360, 290);
    	}
    }
    
    public void gravity(Character ch, GameContainer gc) //can re-add delta if needed
    {
    	ch.y -= ch.ySpeed; //move character based on previous speed adjustments
    	
    	if(!ch.isOnFloor(lvl1, gc)) //if not on ground, then jumping
    	{
    		//System.out.println("gravity is applied");
    		ch.gCount -= 0.2; //increments gravity counter when in air
    		
    		ch.y -= ch.gCount; //negative acceleration
    		//allows that character falls smoothly when w is not held:
    		ch.ySpeed -= 0.5; //decreases velocity up unless jump key is held
    		
    		//check for head collision a second time:
    		
    		if(ch.checkCollision(lvl1, gc, "up")) //push player down if head bumps
			{
				ch.gCount--;
			}
    	}
    	
    	//***consider adjusting one time to top of block instead
		//checks if on or below the floor after gravity, and adjusts:
    	if(ch.isOnFloor(lvl1, gc))
    	{
    		//System.out.println("is on floor");
    		ch.gCount = 0; //reset gravity counter
    		ch.ySpeed = 0; //reset speed counter
    		while(ch.isOnFloor(lvl1,  gc)) //if below floor, move up until not
    		{
    			ch.y--; //checks by pixel
    			//System.out.println("is below floor");
    		}
    		ch.y++;
    	}
    	
		//System.out.println("y velocity = " + (ch.ySpeed+ch.gCount));
    }
    
    public void wall(Character ch, PlatformLevel lvl) //wall barrier
	{
		if(ch.x < 0)
		{
			ch.x = 0;
		}
		if(ch.x+ch.width > lvl.floorLength)
		{
			ch.x = lvl.floorLength - ch.width;
		}
	}
}