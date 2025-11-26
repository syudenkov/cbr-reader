# CBR/CBZ/JPG Reader Application - Questions

Please answer these questions to help create a more accurate technical analysis and architecture design.

---

## 1. User Experience & Features

### 1.1 Offline Capabilities
- Should the app support offline reading (download comics/images for offline use)?
[Answer] yes, why not
- If yes, should users be able to download entire series or just individual comics?
[Answer] individual CBR/CBZ/JPEG set

### 1.2 Parental Controls
- Do you want parental controls or content filtering?
[Answer] Parental control - no, filtering - yes
- Should there be age ratings for content?
[Answer] No
- Reading time limits or usage monitoring?
[Answer] No

### 1.3 User Accounts
- Should kids have individual profiles/accounts, or anonymous access?
[Answer] There will be profile of the user to have history/favorites/settings and other features
- Should there be parent accounts separate from child accounts?
[Answer] No
- Do you need family account features (one parent managing multiple children)?
[Answer] No

### 1.4 Reading Features
- Which reading modes are important?
  - Single page view
  - Double page spread (for landscape)  
  - Continuous scroll mode
  - Vertical/horizontal reading direction
[Answer] All mentioned
- Navigation preferences:
  - Page thumbnails/grid view
  - Table of contents
  - Quick jump to page number
[Answer] All mentioned
- Zoom and pan functionality priority (essential vs. nice-to-have)?
[Answer] Essential

### 1.5 Progress Tracking
- Bookmarking and reading progress tracking needed?
[Answer] yes
- Should progress sync across devices?
[Answer] yes
- Reading history tracking?
[Answer] yes

---

## 2. OCR & Text-to-Speech

### 2.1 OCR Behavior
- Should OCR happen automatically for all uploaded images, or on-demand when user requests?
[Answer] On demand since it is expensive. The final audio should be stored to not process it again
- Should the app detect comic panels/speech bubbles automatically?
[Answer] No, it will be done on the LLM side
- Do you need manual correction capability if OCR makes mistakes?
[Answer] No, but there should be option to mark such comics as problematic, so later they could be reviewed and reprocessed via web UI of admin

### 2.2 Language Support
- Primary language: English only or multiple languages?
[Answer] Multiple: English, Russian, Ukrainian, Belarussian
- If multiple, which languages are priority?
[Answer] Ukrainian

### 2.3 TTS Features
- Should the app highlight text as it's being read (karaoke-style)?
[Answer] No
- Reading modes:
  - Tap individual panels to hear text
  [Answer] Yes
  - Auto-play entire page sequentially
  [Answer] Yes
  - Continuous reading mode (reads whole comic)
  [Answer] Yes
- Voice options:
  - Single narrator voice
  - Character-based voices (if possible)
  - Male/female voice options
  - Child-friendly voices priority?
[Answer] All mentioned options  
- Adjustable reading speed needed?
[Answer] no

### 2.4 OCR Storage
- Should OCR text be stored permanently or processed each time?
[Answer] Perma
- Should users be able to view the extracted text separately (accessibility)?
[Answer] No, only by admins in web UI
---

## 3. Content Management

### 3.1 Content Upload
- Who will upload content?
[Answer] Admins via web UI, it will be rather a manual process of uploading files to the storage and then the backend app will scan the storage and read the dirs 
  - Parents upload for their kids
  - Admins manage a central library
  - Both options
- Content moderation needed before it's available?
[Answer] No

### 3.2 Content Organization
- Age-appropriate content categorization needed?
[Answer] No
- Genres/categories for comics (superhero, educational, adventure, etc.)?
[Answer] Yes, manual tagging will be necessary
- Series/collection grouping?
[Answer] Yes
- Author/publisher metadata?
[Answer] Yes

### 3.3 Storage Estimates
- Expected file size per comic (rough estimate)?
[Answer] 2-200MB
- Expected number of comics in the library initially?
[Answer] 600+
- Expected growth over first year?
[Answer] up to 10k issues

### 3.4 Concurrent Users
- Expected number of concurrent users (10s, 100s, 1000s)?
[Answer] 10
- Is this for a family (2-5 users), school (100s), or public service (1000s+)?
[Answer] family

---

## 4. Platform & Deployment

### 4.1 Platform Priority
- Primary target: web app first, native Android app first, or both simultaneously?
[Answer] web app first, when it is tested - android app could be created
- If web app: should it be a Progressive Web App (installable)?
[Answer] No
- Future iOS support needed?
[Answer] No

### 4.2 Device Targets
- Tablet sizes: 7", 10", or both?
[Answer] 10+
- Should it also work on smartphones (smaller screens)?
[Answer] Yes, landscape 
- Minimum Android version support needed?
[Answer] No

### 4.3 Cloud Provider
- Do you have preference for specific cloud provider?
  - AWS (Amazon Web Services)
  - GCP (Google Cloud Platform)
  [Answer] Yes
  - Azure (Microsoft)
  - Other (DigitalOcean, Linode, self-hosted)
  - No preference (recommend best option)

### 4.4 Budget
- What's your budget category?
  - Hobby/side project (~$20-50/month)
  [Answer] Yes
  - Startup/small business (~$100-300/month)
  - Professional/enterprise (~$500+/month)
  - Need cost optimization as top priority
- Any concerns about per-use costs (OCR/TTS can be expensive at scale)?
[Answer] No, since it is for own family

---

## 5. Technical Constraints & Preferences

### 5.1 Existing Infrastructure
- Any existing infrastructure or systems to integrate with?
[Answer] No
- Preferred programming languages or frameworks?
[Answer] Java/Spring, Groovy, Vanilla JS, React, Mysql, Postgres, SQLLite, docker, gcp
- Development team experience (if any)?
[Answer] Yes, I am techlead

### 5.2 Data Privacy & Compliance
- Geographic location of primary users (affects data residency)?
[Answer] No
- Data privacy requirements:
  - COPPA compliance needed (kids under 13 in USA)?
  - GDPR compliance needed (European users)?
  - Other regulatory requirements?
[Answer] No

### 5.3 Technical Preferences
- Backend preference:
  - Node.js/TypeScript
  - Python (FastAPI, Django)
  - Java/Spring Boot
  - No preference
- Frontend preference:
  - React
  - Vue.js
  - Angular
  - Flutter (cross-platform)
  - No preference
[Answer] No preferences
---

## 6. Project Scope & Timeline

### 6.1 Timeline
- Desired timeline for MVP (minimum viable product)?
[Answer] No
- Hard deadline or flexible development schedule?
[Answer] No

### 6.2 MVP vs. Full Product
- Which features are MUST-HAVE for initial launch?
[Answer] I do not know. Decide on your own or suggest options.
- Which features can be added later?
[Answer] I do not know. Decide on your own or suggest options.

### 6.3 Development Resources
- How many developers available?
[Answer] Myself + Claude Code
- In-house development or outsourced?
- Need help with DevOps/infrastructure setup?
[Answer] No

---

## 7. Additional Features & Considerations

### 7.1 Monetization
- Is this a free service, subscription-based, or ad-supported?
[Answer] Free
- Any in-app purchases planned?
[Answer] No

### 7.2 Social/Sharing Features
- Should kids be able to share favorite comics with friends (within app)?
[Answer] No
- Reading achievements or gamification (badges, streaks)?
[Answer] No
- Comments or ratings on comics?
[Answer] Yes

### 7.3 Analytics
- What metrics are important to track?
  - Reading completion rates
  - Popular content
  - User engagement time
  - OCR/TTS usage statistics
  - Other?
[Answer] All

### 7.4 Accessibility
- Beyond TTS, any other accessibility features needed?
  - High contrast mode
  - Dyslexia-friendly fonts
  - Screen reader support
[Answer] No
---

## 8. Content Source & Copyright

### 8.1 Content Licensing 
[Answer] Is not considered in this product
- Will content be:
  - User-uploaded only
  - Licensed from publishers
  - Mix of both
  - Original content only
- Copyright/DMCA compliance considerations?

### 8.2 Content Quality
- Are comics professional/published or also user-created?
- Image quality expectations (resolution requirements)?
[Answer] No

---

## 9. Anything Else?

- Any other specific requirements, concerns, or ideas not covered above?
[Answer] No
- Any specific examples of apps you like or want to emulate?
[Answer] No
- Deal-breakers or must-avoid technologies?
[Answer] No

---

## How to Use This File

1. Answer the questions that are most important to your project
2. Skip questions that aren't relevant
3. Add any additional context or requirements at the end
4. Save this file and let me know when you're done

I'll then create a comprehensive technical analysis tailored to your specific needs.
