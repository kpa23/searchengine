package searchengine.services;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;
import searchengine.repository.Utils;

import java.io.IOException;
import java.lang.invoke.WrongMethodTypeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveTask;


public class PageParse extends RecursiveTask<List<String>> {
    private static final Logger logger = LogManager.getLogger(PageParse.class);

    private final String url;
    private final String domain;
    private PageParse parent;
    private final List<PageParse> links;
    private int level;
    private final SiteT siteT;
    private final PageTRepository pageTRepository;
    private final SiteTRepository siteTRepository;
    private final ConcurrentMap<String, PageParse> uniqueLinks;
    private Connection.Response response = null;

    public PageParse(String url, PageParse parent) {
        this(url, parent.domain, parent.siteT, parent.pageTRepository, parent.siteTRepository, parent.uniqueLinks);
        this.parent = parent;
        this.level = parent.level + 1;
    }

    public PageParse(String url, String domain, SiteT siteT
            , PageTRepository pageTRepository, SiteTRepository siteTRepository
            , ConcurrentMap<String, PageParse> uniqueLinks) {
        this.url = url;
        this.domain = domain;
        this.parent = null;
        this.links = new ArrayList<>();
        this.level = 0;
        this.siteT = siteT;
        this.pageTRepository = pageTRepository;
        this.siteTRepository = siteTRepository;
        this.uniqueLinks = Objects.requireNonNullElseGet(uniqueLinks, ConcurrentHashMap::new);
    }

    @Override
    protected List<String> compute() {
        List<String> list = new ArrayList<>();
        List<PageParse> tasks = new ArrayList<>();

        Document document;
        uniqueLinks.put("/", this);

        try {
            document = downloadAndSavePage();
            if (document == null) {
                throw new NullPointerException("document null");
            }
            document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
            Elements elements = document.select("a[href~=^/?([\\w\\d/-]+)?]");
            parseAllHrefs(elements, list, tasks);
            addResultsFromTasks(list, tasks);
        } catch (IOException e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
        } catch (NullPointerException ignored) {//
        }

        return list;
    }

    public Document downloadAndSavePage() throws IOException {
        Document document = null;
        try {
            response = Jsoup.connect(domain + url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) YandexIndexingMachine")
//                    .userAgent(parse.getUseragent())
                    .referrer("https://www.google.com")
                    .ignoreContentType(true)
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();
            if (!response.contentType().startsWith("text/html;")) {
                throw new WrongMethodTypeException("wrong format");
            }
            if (response.statusCode() != 200) throw new IOException(String.valueOf(response.statusCode()));
            document = response.parse();
            savePage(document.body().text(), document.title());

        } catch (IOException e) {
            savePage();
        } catch (WrongMethodTypeException e) { //
        }
        return document;
    }

    private void parseAllHrefs(Elements elements, List<String> list, List<PageParse> tasks) {
        for (Element e : elements) {
            String href = e.attr("href").replace("//www.", "//");
            if (!href.startsWith(this.domain) &&
                    (href.startsWith("http") || href.isEmpty() || href.contains("#") || href.contains(":")))
                continue;
            String checkUrl = generateUrl(href).replace(domain, "");
            if (!checkAddUrl(checkUrl)) {
                list.add(checkUrl);
                PageParse newParse = new PageParse(checkUrl, this);
                newParse.fork();
                tasks.add(newParse);
                links.add(newParse);
            }
        }
    }

    public PageT savePage(String text, String title) {

        int code;
        if (response == null || (text.equals(""))) {
            code = 408;
        } else {
            code = response.statusCode();
        }
        PageT p = new PageT(siteT.getSiteId(), url, code, text, title);
        pageTRepository.save(p);
        siteT.setStatusTime(Utils.getTimeStamp());
        siteTRepository.save(siteT);
        return p;
    }

    public PageT savePage() {
        return savePage("", "");

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t".repeat(this.level)).append(this.url).append("\n");
        this.links.forEach(e -> sb.append(e.toString()));

        return sb.toString();
    }

    private void addResultsFromTasks(List<String> list, List<PageParse> tasks) {
        for (PageParse item : tasks) {
            list.addAll(item.join());
        }
    }

    private boolean checkAddUrl(String url) {
//        AtomicBoolean isExist = new AtomicBoolean(true);
//        uniqueLinks.computeIfAbsent(url, e -> {
//            uniqueLinks.put(url, this);
//            isExist.set(false);
//            return null;
//        });
        boolean isExist = uniqueLinks.containsKey(url);
        if (!isExist) {
            uniqueLinks.put(url, this);
        }
        return isExist;
    }

    private String generateUrl(String href) {
        String result;
        if (!href.startsWith(this.domain)) {
            if (!href.startsWith("/")) {
                if (!url.endsWith("/")) {
                    if (this.domain.equals(url)) {
                        result = url + "/" + href;
                    } else {
                        result = url.substring(0, url.lastIndexOf("/") + 1) + href;
                    }
                } else {
                    result = url + href;
                }
            } else {
                result = this.domain + href;
            }
        } else {
            result = href;
        }
        if (result.indexOf("/?") > 0) {
            result = result.substring(0, result.indexOf("/?") + 1);
        }
        return result;
    }
}
