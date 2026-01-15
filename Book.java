/**
 * Book - Represents a book in the catalogue
 */
public class Book {
    private String title;
    private int price;

    /**
     * Constructor
     * @param title The book title
     * @param price The book price
     */
    public Book(String title, int price) {
        this.title = title;
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return title + " (Price: " + price + ")";
    }
}
