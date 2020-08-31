package com.bawarchef.DataStructures;

public class Queue<T> {
    Node<T> start=null,end=null;
    long count = 0;

    public Queue(){
    }

    public void push(T data){
        Node<T> n = new Node<T>(data,null,null);
        if(count==0) {
            start = n;
            end = n;
        } else{
            end.next = n;
            n.prev = end;
            end = n;
        }
        count++;
    }

    public T getNextMessageUnPopped(){
        if(!isEmpty())
            return start.data;
        else
            return null;
    }

    public T getNextMessagePopped(){
        if(!isEmpty()) {
            T d = start.data;
            start = start.next;
            if(start!=null) start.prev = null;
            else end=null;
            count--;
            return d;
        }
        else {
            return null;
        }
    }

    public boolean isEmpty(){
        return count==0;
    }

    public long getCount(){
        return count;
    }



}

class Node<T>{
    T data=null;
    Node<T> next=null,prev=null;

    Node(T data, Node<T> next, Node<T> prev){
        this.data = data;
        this.next = next;
        this.prev = prev;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Node<T> getNext() {
        return next;
    }

    public void setNext(Node<T> next) {
        this.next = next;
    }

    public Node<T> getPrev() {
        return prev;
    }

    public void setPrev(Node<T> prev) {
        this.prev = prev;
    }
}
