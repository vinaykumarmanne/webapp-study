package ai.elimu.web.content.storybook.paragraph;

import ai.elimu.dao.AudioContributionEventDao;
import ai.elimu.dao.AudioDao;
import ai.elimu.dao.StoryBookContributionEventDao;
import ai.elimu.dao.StoryBookDao;
import org.apache.logging.log4j.Logger;
import ai.elimu.dao.StoryBookParagraphDao;
import ai.elimu.model.content.StoryBook;
import ai.elimu.model.content.StoryBookParagraph;
import ai.elimu.model.content.multimedia.Audio;
import ai.elimu.model.contributor.AudioContributionEvent;
import ai.elimu.model.contributor.Contributor;
import ai.elimu.model.contributor.StoryBookContributionEvent;
import ai.elimu.model.enums.Language;
import ai.elimu.model.enums.PeerReviewStatus;
import ai.elimu.model.enums.Platform;
import ai.elimu.model.enums.content.AudioFormat;
import ai.elimu.rest.v2.service.StoryBooksJsonService;
import ai.elimu.util.ConfigHelper;
import ai.elimu.util.audio.GoogleCloudTextToSpeechHelper;
import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/content/storybook/paragraph/edit")
public class StoryBookParagraphEditController {
    
    private final Logger logger = LogManager.getLogger();
    
    @Autowired
    private StoryBookDao storyBookDao;
    
    @Autowired
    private StoryBookContributionEventDao storyBookContributionEventDao;
    
    @Autowired
    private StoryBookParagraphDao storyBookParagraphDao;
    
    @Autowired
    private AudioDao audioDao;
    
    @Autowired
    private AudioContributionEventDao audioContributionEventDao;
    
    @Autowired
    private StoryBooksJsonService storyBooksJsonService;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String handleRequest(Model model, @PathVariable Long id, HttpSession session) {
    	logger.info("handleRequest");
        
        StoryBookParagraph storyBookParagraph = storyBookParagraphDao.read(id);
        logger.info("storyBookParagraph: " + storyBookParagraph);
        model.addAttribute("storyBookParagraph", storyBookParagraph);
        
        // Generate Audio for this StoryBookParagraph (if it has not been done already)
        if (storyBookParagraph.getAudio() == null) {
            Calendar timeStart = Calendar.getInstance();
            Language language = Language.valueOf(ConfigHelper.getProperty("content.language"));
            try {
                byte[] audioBytes = GoogleCloudTextToSpeechHelper.synthesizeText(storyBookParagraph.getOriginalText(), language);
                logger.info("audioBytes: " + audioBytes);
                if (audioBytes != null) {
                    Audio audio = new Audio();
                    audio.setTimeLastUpdate(Calendar.getInstance());
                    audio.setContentType(AudioFormat.MP3.getContentType());
                    audio.setStoryBookParagraph(storyBookParagraph);
                    audio.setTitle(
                            "storybook-" + storyBookParagraph.getStoryBookChapter().getStoryBook().getId() + 
                            "-ch-" + (storyBookParagraph.getStoryBookChapter().getSortOrder() + 1) + 
                            "-par-" + (storyBookParagraph.getSortOrder() + 1)
                    );
                    audio.setTranscription(storyBookParagraph.getOriginalText());
                    audio.setBytes(audioBytes);
                    audio.setDurationMs(null); // TODO: Convert from byte[] to File, and extract audio duration
                    audio.setAudioFormat(AudioFormat.MP3);
                    audioDao.create(audio);
                    
                    storyBookParagraph.setAudio(audio);
                    storyBookParagraphDao.update(storyBookParagraph);
                    
                    AudioContributionEvent audioContributionEvent = new AudioContributionEvent();
                    audioContributionEvent.setContributor((Contributor) session.getAttribute("contributor"));
                    audioContributionEvent.setTime(Calendar.getInstance());
                    audioContributionEvent.setAudio(audio);
                    audioContributionEvent.setRevisionNumber(audio.getRevisionNumber());
                    audioContributionEvent.setComment("Google Cloud Text-to-Speech (🤖 auto-generated comment)️");
                    audioContributionEvent.setTimeSpentMs(System.currentTimeMillis() - timeStart.getTimeInMillis());
                    audioContributionEvent.setPlatform(Platform.WEBAPP);
                    audioContributionEventDao.create(audioContributionEvent);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
        
        model.addAttribute("audios", audioDao.readAllOrderedByTitle());
        
        model.addAttribute("timeStart", System.currentTimeMillis());
        
        return "content/storybook/paragraph/edit";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public String handleSubmit(
            HttpServletRequest request,
            HttpSession session,
            @Valid StoryBookParagraph storyBookParagraph,
            BindingResult result,
            Model model
    ) {
    	logger.info("handleSubmit");
        
        Contributor contributor = (Contributor) session.getAttribute("contributor");
        
        if (result.hasErrors()) {
            model.addAttribute("storyBookParagraph", storyBookParagraph);
            model.addAttribute("audios", audioDao.readAllOrderedByTitle());
            model.addAttribute("timeStart", System.currentTimeMillis());
            return "content/storybook/paragraph/edit";
        } else {
            storyBookParagraphDao.update(storyBookParagraph);
            
            // Update the storybook's metadata
            StoryBook storyBook = storyBookParagraph.getStoryBookChapter().getStoryBook();
            storyBook.setTimeLastUpdate(Calendar.getInstance());
            storyBook.setRevisionNumber(storyBook.getRevisionNumber() + 1);
            storyBook.setPeerReviewStatus(PeerReviewStatus.PENDING);
            storyBookDao.update(storyBook);
            
            // Store contribution event
            StoryBookContributionEvent storyBookContributionEvent = new StoryBookContributionEvent();
            storyBookContributionEvent.setContributor(contributor);
            storyBookContributionEvent.setTime(Calendar.getInstance());
            storyBookContributionEvent.setStoryBook(storyBook);
            storyBookContributionEvent.setRevisionNumber(storyBook.getRevisionNumber());
            storyBookContributionEvent.setComment("Edited storybook paragraph (🤖 auto-generated comment)");
            storyBookContributionEvent.setTimeSpentMs(System.currentTimeMillis() - Long.valueOf(request.getParameter("timeStart")));
            storyBookContributionEventDao.create(storyBookContributionEvent);
            
            // Refresh the REST API cache
            storyBooksJsonService.refreshStoryBooksJSONArray();
            
            return "redirect:/content/storybook/edit/" + 
                    storyBookParagraph.getStoryBookChapter().getStoryBook().getId() + 
                    "#ch-id-" + storyBookParagraph.getStoryBookChapter().getId();
        }
    }
}
