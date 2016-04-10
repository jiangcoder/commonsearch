package org.web;
import java.util.Collections;
import java.util.Stack;

/**
 * Created by karak on 16-3-12.
 * 两个栈实现一个队列
 */
public class StackQueue {
    Stack<Character> characters1=new Stack<Character>();
    Stack<Character> characters2=new Stack<Character>();

    void Enqueue(Character c) {
        characters1.push(c);
    }

    Character Dequeue() {
        if (characters2.empty()) {
            if (characters1.empty()) {
                return '\0';
            } else {
                Stack<Character> tmp;
                tmp = characters1;
                characters1 = characters2;
                characters2 = tmp;
                Collections.reverse(characters2);
                return characters2.pop();
            }
        } else {
            return characters2.pop();
        }
    }

    public static void main(String[] args) {
        StackQueue s=new StackQueue();
        s.Enqueue('a');
        s.Enqueue('b');
        s.Enqueue('c');
        s.Enqueue('d');
        Character c;
        while((c=s.Dequeue())!='\0'){
            System.out.println(c);
        }

    }
}