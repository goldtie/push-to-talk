package org.sipdroid.sipua.ui.settings.basic;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.sipdroid.sipua.R;

import android.util.Log;

public class HelpActivity extends ExpandableListActivity
{
    private static final String LOG_TAG = "SIPDROID";

    static final String questions[] = {
	  "grey",
	  "blue",
	  "yellow",
	  "red"
	};

	static final String shades[][] = {
// Shades of grey
	  {
		  "So shaken as we are, so wan with care," +
          "Find we a time for frighted peace to pant," +
          "And breathe short-winded accents of new broils" +
          "To be commenced in strands afar remote." +
          "No more the thirsty entrance of this soil" +
          "Shall daub her lips with her own children's blood;" +
          "Nor more shall trenching war channel her fields," +
          "Nor bruise her flowerets with the armed hoofs" +
          "Of hostile paces: those opposed eyes," +
          "Which, like the meteors of a troubled heaven," +
          "All of one nature, of one substance bred," +
          "Did lately meet in the intestine shock" +
          "And furious close of civil butchery" +
          "Shall now, in mutual well-beseeming ranks," +
          "March all one way and be no more opposed" +
          "Against acquaintance, kindred and allies:" +
          "The edge of war, like an ill-sheathed knife," +
          "No more shall cut his master. Therefore, friends," +
          "As far as to the sepulchre of Christ," +
          "Whose soldier now, under whose blessed cross" +
          "We are impressed and engaged to fight," +
          "Forthwith a power of English shall we levy;" +
          "Whose arms were moulded in their mothers' womb" +
          "To chase these pagans in those holy fields" +
          "Over whose acres walk'd those blessed feet" +
          "Which fourteen hundred years ago were nail'd" +
          "For our advantage on the bitter cross." +
          "But this our purpose now is twelve month old," +
          "And bootless 'tis to tell you we will go:" +
          "Therefore we meet not now. Then let me hear" +
          "Of you, my gentle cousin Westmoreland," +
          "What yesternight our council did decree" +
          "In forwarding this dear expedience."
		
	  },
// Shades of blue
	  {
		  "Hear him but reason in divinity," + 
          "And all-admiring with an inward wish" + 
          "You would desire the king were made a prelate:" + 
          "Hear him debate of commonwealth affairs," + 
          "You would say it hath been all in all his study:" + 
          "List his discourse of war, and you shall hear" + 
          "A fearful battle render'd you in music:" + 
          "Turn him to any cause of policy," + 
          "The Gordian knot of it he will unloose," + 
          "Familiar as his garter: that, when he speaks," + 
          "The air, a charter'd libertine, is still," + 
          "And the mute wonder lurketh in men's ears," + 
          "To steal his sweet and honey'd sentences;" + 
          "So that the art and practic part of life" + 
          "Must be the mistress to this theoric:" + 
          "Which is a wonder how his grace should glean it," + 
          "Since his addiction was to courses vain," + 
          "His companies unletter'd, rude and shallow," + 
          "His hours fill'd up with riots, banquets, sports," + 
          "And never noted in him any study," + 
          "Any retirement, any sequestration" + 
          "From open haunts and popularity."
		
	  },
// Shades of yellow
	  {
		  "I come no more to make you laugh: things now," +
          "That bear a weighty and a serious brow," +
          "Sad, high, and working, full of state and woe," +
          "Such noble scenes as draw the eye to flow," +
          "We now present. Those that can pity, here" +
          "May, if they think it well, let fall a tear;" +
          "The subject will deserve it. Such as give" +
          "Their money out of hope they may believe," +
          "May here find truth too. Those that come to see" +
          "Only a show or two, and so agree" +
          "The play may pass, if they be still and willing," +
          "I'll undertake may see away their shilling" +
          "Richly in two short hours. Only they" +
          "That come to hear a merry bawdy play," +
          "A noise of targets, or to see a fellow" +
          "In a long motley coat guarded with yellow," +
          "Will be deceived; for, gentle hearers, know," +
          "To rank our chosen truth with such a show" +
          "As fool and fight is, beside forfeiting" +
          "Our own brains, and the opinion that we bring," +
          "To make that only true we now intend," +
          "Will leave us never an understanding friend." +
          "Therefore, for goodness' sake, and as you are known" +
          "The first and happiest hearers of the town," +
          "Be sad, as we would make ye: think ye see" +
          "The very persons of our noble story" +
          "As they were living; think you see them great," +
          "And follow'd with the general throng and sweat" +
          "Of thousand friends; then in a moment, see" +
          "How soon this mightiness meets misery:" +
          "And, if you can be merry then, I'll say" +
          "A man may weep upon his wedding-day."
		
	  },
// Shades of red
	  {
		  "First, heaven be the record to my speech!" + 
          "In the devotion of a subject's love," + 
          "Tendering the precious safety of my prince," + 
          "And free from other misbegotten hate," + 
          "Come I appellant to this princely presence." + 
          "Now, Thomas Mowbray, do I turn to thee," + 
          "And mark my greeting well; for what I speak" + 
          "My body shall make good upon this earth," + 
          "Or my divine soul answer it in heaven." + 
          "Thou art a traitor and a miscreant," + 
          "Too good to be so and too bad to live," + 
          "Since the more fair and crystal is the sky," + 
          "The uglier seem the clouds that in it fly." + 
          "Once more, the more to aggravate the note," + 
          "With a foul traitor's name stuff I thy throat;" + 
          "And wish, so please my sovereign, ere I move," + 
          "What my tongue speaks my right drawn sword may prove."
		
	  }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.help_activity);
		SimpleExpandableListAdapter expListAdapter =
			new SimpleExpandableListAdapter(
				this,
				createGroupList(),	// groupData describes the first-level entries
				R.layout.help_activity_group_row,	// Layout for the first-level entries
				new String[] { "question" },	// Key in the groupData maps to display
				new int[] { R.id.childname },		// Data under "colorName" key goes into this TextView
				createChildList(),	// childData describes second-level entries
				R.layout.help_activity_child_row,	// Layout for second-level entries
				new String[] { "answer"},	// Keys in childData maps to display
				new int[] { R.id.childname1}	// Data under the keys above go into these TextViews
			);
		setListAdapter( expListAdapter );
    }

    public void  onContentChanged  () {
        super.onContentChanged();
        Log.d( LOG_TAG, "onContentChanged" );
    }

    public boolean onChildClick(
            ExpandableListView parent, 
            View v, 
            int groupPosition,
            int childPosition,
            long id) {
        Log.d( LOG_TAG, "onChildClick: "+childPosition );
       
        return false;
    }

    public void  onGroupExpand  (int groupPosition) {
        Log.d( LOG_TAG,"onGroupExpand: "+groupPosition );
    }

/**
  * Creates the group list out of the colors[] array according to
  * the structure required by SimpleExpandableListAdapter. The resulting
  * List contains Maps. Each Map contains one entry with key "colorName" and
  * value of an entry in the colors[] array.
  */
	private List<HashMap<String, String>> createGroupList() {
	  ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
	  for( int i = 0 ; i < questions.length ; ++i ) {
		HashMap<String, String> m = new HashMap<String, String>();
	    m.put( "question",questions[i] );
		result.add( m );
	  }
	  return (List<HashMap<String, String>>)result;
    }


  private List<ArrayList<HashMap<String, String>>> createChildList() {
	ArrayList<ArrayList<HashMap<String, String>>> result = new ArrayList<ArrayList<HashMap<String, String>>>();
	for( int i = 0 ; i < shades.length ; ++i ) {
// Second-level lists
	  ArrayList<HashMap<String, String>> secList = new ArrayList<HashMap<String, String>>();
	  for( int n = 0 ; n < shades[i].length ; n += 1 ) {
	    HashMap<String, String> child = new HashMap<String, String>();
		child.put( "answer", shades[i][n] );
	    
		secList.add( child );
	  }
	  result.add( secList );
	}
	return result;
  }

}
