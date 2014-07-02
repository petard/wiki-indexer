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

    /**
     * Polls queue for latest parser wiki pages
     */
    T pollQueue();

    /**
     *
     * @return true if work is done
     */
    boolean isDone();
}
