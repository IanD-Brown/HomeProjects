#pragma once

#include <queue>
#include <set>
#include <utility>
#include <functional>
#include <limits>
#include <map>
#include <vector>

template <typename T> class Graph {
public:
    T m_nodeCount;
    std::vector<std::vector<std::pair<T, int>>> m_edges;
    std::map<T, std::set<T>> m_bestNextNode;
    T m_src;

    Graph(T nodeCount) : m_edges(nodeCount), m_src(-1) {
        m_nodeCount = nodeCount;
    }

    void addEdge(T from, T to, int weight) { addEdge(from, to, weight, weight); }

    void addEdge(T from, T to, int weight, int reverseWeight) {
        m_edges[from].push_back(make_pair(to, weight));
        m_edges[to].push_back(make_pair(from, reverseWeight));
    }

    void dijkstra(T src) {
        if (m_src != src) {
            m_src = src;
            std::priority_queue<std::pair<int, T>, std::vector<std::pair<int, T>>, greater<std::pair<int, T>>>
                pq;
            std::vector<int> dist(m_nodeCount, numeric_limits<int>::max());

            pq.push(make_pair(0, src));
            dist[src] = 0;

            while (!pq.empty()) {
                T from = pq.top().second;
                pq.pop();

                for (const auto& it : m_edges[from]) {
                    T to(it.first);
                    int weight(it.second + dist[from]);

                    if (weight < dist[to]) {
                        dist[to] = weight;
                        m_bestNextNode[to] = { from };
                        pq.push(make_pair(dist[to], to));
                    }
                    else if (weight == dist[to]) {
                        m_bestNextNode[to].insert(from);
                    }
                }
            }
        }
    }

    std::vector<std::vector<T>> pathsTo(T dest) {
        std::vector<std::vector<T>> paths;

        if (dest == m_src) {
            paths = { {m_src} };
        }
        else {
            for (T nextNode : m_bestNextNode[dest]) {
                for (std::vector<T> nextNodePath : pathsTo(nextNode)) {
                    std::vector<T> pathToAdd(nextNodePath);
                    pathToAdd.insert(pathToAdd.begin(), dest);
                    paths.push_back(pathToAdd);
                }
            }
        }

        return paths;
    }
};
