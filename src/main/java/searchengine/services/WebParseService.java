package searchengine.services;

import searchengine.model.PageT;

public interface WebParseService  {

    public void parseAllPages();

    public void parseSinglePage(PageT pageT);

}
