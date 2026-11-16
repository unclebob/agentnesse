package agnesse.components;

public interface Traverser<T> {

  void traverse(TraversalListener<T> traversalListener);

}
