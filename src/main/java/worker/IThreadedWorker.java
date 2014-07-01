package worker;

/**
 * User: cajias
 * Date: 01.07.14
 * Time: 21:55
 */
public interface IThreadedWorker<T> {

    /**
     * Starts parsers thread
     * @throws Exception
     */
    void start() throws Exception;

    int processCount();

    /**
     * Polls queue for latest parser wiki pages
     */
    T pollQueue();
}
